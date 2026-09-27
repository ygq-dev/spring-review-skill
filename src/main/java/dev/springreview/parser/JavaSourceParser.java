package dev.springreview.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Problem;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import dev.springreview.observability.Logs;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class JavaSourceParser {

    private static final Logger LOG = Logs.logger("java-parser");

    public ParsedSource parse(SourceUnit unit) {
        Logs.module("M09");
        String path = unit.path();
        String hash = unit.hash();
        if (unit.content() == null || unit.content().isEmpty()) {
            return emptySource(unit, ParseError.skip(path, "empty content", hash));
        }

        JavaParser parser = new JavaParser(defaultConfiguration());
        ParseResult<CompilationUnit> result;
        try {
            result = parser.parse(unit.content());
        } catch (RuntimeException ex) {
            LOG.warn("parse threw for {}: {}", path, ex.getMessage());
            return emptySource(unit, ParseError.internal(path, ex.getMessage(), hash));
        }

        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            List<ParseError> errors = new ArrayList<>();
            for (Problem p : result.getProblems()) {
                int line = 0;
                int col = 0;
                String loc = "0:0";
                Optional<TokenRange> trOpt = p.getLocation();
                if (trOpt.isPresent()) {
                    Optional<Range> rOpt = trOpt.get().getBegin().getRange();
                    if (rOpt.isPresent()) {
                        Position pos = rOpt.get().begin;
                        line = pos.line;
                        col = pos.column;
                        loc = line + ":" + col;
                    }
                }
                errors.add(ParseError.syntax(path, line, col,
                    p.getMessage() + " @" + loc, hash));
            }
            if (errors.isEmpty()) {
                errors.add(ParseError.syntax(path, 0, 0, "parse failed", hash));
            }
            return new ParsedSource(path, hash, "", List.of(), List.of(), List.of(),
                DependencyGraph.empty(), List.copyOf(errors), null);
        }

        CompilationUnit cu = result.getResult().get();
        String pkg = cu.getPackageDeclaration()
            .map(pd -> pd.getNameAsString())
            .orElse("");

        List<ParsedSource.ImportInfo> imports = new ArrayList<>();
        for (ImportDeclaration id : cu.getImports()) {
            imports.add(new ParsedSource.ImportInfo(
                id.getNameAsString(),
                id.isStatic(),
                id.isAsterisk(),
                id.getBegin().map(p -> p.line).orElse(0)));
        }

        List<ParsedSource.TypeInfo> types = collectTypes(cu, pkg);
        List<ParsedSource.MethodInfo> methods = collectMethods(cu, pkg);
        DependencyGraph graph = analyzePackageDeps(cu, pkg);

        return new ParsedSource(path, hash, pkg,
            List.copyOf(imports),
            List.copyOf(types),
            List.copyOf(methods),
            graph,
            List.of(),
            cu);
    }

    public DependencyGraph analyzePackageDeps(ParsedSource source) {
        return source.dependencyGraph();
    }

    public ParsedBundle parseAll(SourceBundle bundle) {
        Logs.module("M09");
        Instant start = Instant.now();
        List<ParsedSource> sources = new ArrayList<>();
        List<ParseError> allErrors = new ArrayList<>();
        Map<String, List<DependencyEdge>> packageEdges = new LinkedHashMap<>();
        Map<String, List<DependencyEdge>> classEdges = new LinkedHashMap<>();
        int typeCount = 0;
        int methodCount = 0;
        int edgeCount = 0;

        for (SourceUnit unit : bundle.units()) {
            if (!"java".equalsIgnoreCase(unit.language())) {
                allErrors.add(ParseError.skip(unit.path(), "non-java", unit.hash()));
                continue;
            }
            ParsedSource ps;
            try {
                ps = parse(unit);
            } catch (RuntimeException ex) {
                LOG.error("parse error for {}: {}", unit.path(), ex.getMessage());
                allErrors.add(ParseError.internal(unit.path(), ex.getMessage(), unit.hash()));
                continue;
            }
            sources.add(ps);
            allErrors.addAll(ps.parseErrors());
            typeCount += ps.types().size();
            methodCount += ps.methods().size();

            for (Map.Entry<String, List<DependencyEdge>> e : ps.dependencyGraph().packageEdges().entrySet()) {
                packageEdges.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).addAll(e.getValue());
                edgeCount += e.getValue().size();
            }
            for (Map.Entry<String, List<DependencyEdge>> e : ps.dependencyGraph().classEdges().entrySet()) {
                classEdges.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).addAll(e.getValue());
                edgeCount += e.getValue().size();
            }
        }

        long dur = Duration.between(start, Instant.now()).toMillis();
        boolean partial = bundle.mode() == dev.springreview.scope.ReviewScope.Mode.DIFF;
        ParsedBundle.Mode mode = partial ? ParsedBundle.Mode.DIFF : ParsedBundle.Mode.FULL;
        ParsedBundle.Stats stats = new ParsedBundle.Stats(
            bundle.units().size(),
            sources.size(),
            bundle.units().size() - sources.size(),
            edgeCount,
            typeCount,
            methodCount,
            dur);

        LOG.info("parsed files={} errors={} types={} methods={} edges={} durationMs={}",
            sources.size(), allErrors.size(), typeCount, methodCount, edgeCount, dur);

        return new ParsedBundle(
            UUID.randomUUID().toString(),
            mode,
            List.copyOf(sources),
            new DependencyGraph(Map.copyOf(packageEdges), Map.copyOf(classEdges)),
            List.copyOf(allErrors),
            stats,
            partial);
    }

    // ---------- 内部 ----------

    private static ParserConfiguration defaultConfiguration() {
        ParserConfiguration cfg = new ParserConfiguration();
        cfg.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        return cfg;
    }

    private static ParsedSource emptySource(SourceUnit unit, ParseError error) {
        return new ParsedSource(unit.path(), unit.hash(), "",
            List.of(), List.of(), List.of(),
            DependencyGraph.empty(), List.of(error), null);
    }

    private static List<ParsedSource.TypeInfo> collectTypes(CompilationUnit cu, String pkg) {
        List<ParsedSource.TypeInfo> out = new ArrayList<>();
        for (TypeDeclaration<?> td : cu.getTypes()) {
            String simple = td.getNameAsString();
            String qualified = pkg.isEmpty() ? simple : pkg + "." + simple;
            String kind = kindOf(td);
            List<String> mods = new ArrayList<>();
            td.getModifiers().forEach(m -> mods.add(m.getKeyword().asString()));
            List<String> annos = new ArrayList<>();
            td.getAnnotations().forEach(a -> annos.add(annotationName(a)));
            String extendsType = null;
            List<String> implementsTypes = new ArrayList<>();
            if (td instanceof ClassOrInterfaceDeclaration cid) {
                extendsType = cid.getExtendedTypes().stream()
                    .findFirst().map(ClassOrInterfaceType::getNameAsString).orElse(null);
                cid.getImplementedTypes().forEach(t -> implementsTypes.add(t.getNameAsString()));
            }
            out.add(new ParsedSource.TypeInfo(
                qualified, simple, kind, List.copyOf(mods), List.copyOf(annos),
                extendsType, List.copyOf(implementsTypes),
                td.getBegin().map(p -> p.line).orElse(0),
                td.getEnd().map(p -> p.line).orElse(0)));
        }
        return out;
    }

    private static String kindOf(TypeDeclaration<?> td) {
        if (td instanceof ClassOrInterfaceDeclaration cid) {
            return cid.isInterface() ? "INTERFACE" : "CLASS";
        }
        if (td instanceof EnumDeclaration) {
            return "ENUM";
        }
        if (td instanceof RecordDeclaration) {
            return "RECORD";
        }
        return "TYPE";
    }

    private static List<ParsedSource.MethodInfo> collectMethods(CompilationUnit cu, String pkg) {
        List<ParsedSource.MethodInfo> out = new ArrayList<>();
        cu.findAll(MethodDeclaration.class).forEach(m -> {
            String owner = ownerOf(m, pkg);
            List<String> params = new ArrayList<>();
            for (Parameter p : m.getParameters()) {
                params.add(p.getTypeAsString());
            }
            List<String> mods = new ArrayList<>();
            m.getModifiers().forEach(md -> mods.add(md.getKeyword().asString()));
            List<String> annos = new ArrayList<>();
            m.getAnnotations().forEach(a -> annos.add(annotationName(a)));
            out.add(new ParsedSource.MethodInfo(
                owner, m.getNameAsString(),
                List.copyOf(params), m.getTypeAsString(),
                List.copyOf(mods), List.copyOf(annos),
                m.getBegin().map(p -> p.line).orElse(0),
                m.getEnd().map(p -> p.line).orElse(0)));
        });
        cu.findAll(ConstructorDeclaration.class).forEach(c -> {
            String owner = ownerOf(c, pkg);
            List<String> params = new ArrayList<>();
            for (Parameter p : c.getParameters()) {
                params.add(p.getTypeAsString());
            }
            List<String> mods = new ArrayList<>();
            c.getModifiers().forEach(md -> mods.add(md.getKeyword().asString()));
            List<String> annos = new ArrayList<>();
            c.getAnnotations().forEach(a -> annos.add(annotationName(a)));
            out.add(new ParsedSource.MethodInfo(
                owner, "<init>",
                List.copyOf(params), owner,
                List.copyOf(mods), List.copyOf(annos),
                c.getBegin().map(p -> p.line).orElse(0),
                c.getEnd().map(p -> p.line).orElse(0)));
        });
        return out;
    }

    private static String ownerOf(Node n, String pkg) {
        return n.findAncestor(TypeDeclaration.class)
            .map(td -> pkg.isEmpty()
                ? td.getNameAsString()
                : pkg + "." + td.getNameAsString())
            .orElse("");
    }

    private static String annotationName(AnnotationExpr a) {
        return a.getNameAsString();
    }

    private static DependencyGraph analyzePackageDeps(CompilationUnit cu, String pkg) {
        if (pkg == null) {
            pkg = "";
        }
        String selfPackage = pkg;
        Map<String, List<DependencyEdge>> packageEdges = new LinkedHashMap<>();
        Map<String, List<DependencyEdge>> classEdges = new LinkedHashMap<>();

        String selfClass = cu.getTypes().isEmpty()
            ? selfPackage
            : (selfPackage.isEmpty()
            ? cu.getType(0).getNameAsString()
            : selfPackage + "." + cu.getType(0).getNameAsString());

        String sourceFile = cu.getStorage()
            .map(s -> s.getFileName()).orElse("");

        for (ImportDeclaration id : cu.getImports()) {
            String full = id.getNameAsString();
            String targetPkg = full.contains(".")
                ? full.substring(0, full.lastIndexOf('.'))
                : full;
            if (targetPkg.equals(selfPackage)) {
                continue;
            }
            int line = id.getBegin().map(p -> p.line).orElse(0);
            int col = id.getBegin().map(p -> p.column).orElse(0);
            DependencyEdge edge = new DependencyEdge(
                selfPackage, targetPkg,
                id.isStatic() ? DependencyEdge.Kind.STATIC_IMPORT
                    : (id.isAsterisk() ? DependencyEdge.Kind.WILDCARD_IMPORT
                    : DependencyEdge.Kind.IMPORT),
                sourceFile, line, col,
                "import " + full);
            packageEdges.computeIfAbsent(selfPackage, k -> new ArrayList<>()).add(edge);

            String targetClass = full;
            DependencyEdge ce = new DependencyEdge(
                selfClass, targetClass,
                edge.kind(),
                sourceFile, line, col,
                "import " + full);
            classEdges.computeIfAbsent(selfClass, k -> new ArrayList<>()).add(ce);
        }

        for (ClassOrInterfaceDeclaration cid : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String self = selfPackage.isEmpty()
                ? cid.getNameAsString()
                : selfPackage + "." + cid.getNameAsString();
            cid.getExtendedTypes().forEach(t -> addClassEdge(classEdges, self, t.getNameAsString(),
                DependencyEdge.Kind.EXTENDS, sourceFile,
                t.getBegin().map(p -> p.line).orElse(0),
                t.getBegin().map(p -> p.column).orElse(0),
                "extends " + t.getNameAsString()));
            cid.getImplementedTypes().forEach(t -> addClassEdge(classEdges, self, t.getNameAsString(),
                DependencyEdge.Kind.IMPLEMENTS, sourceFile,
                t.getBegin().map(p -> p.line).orElse(0),
                t.getBegin().map(p -> p.column).orElse(0),
                "implements " + t.getNameAsString()));
        }

        for (AnnotationExpr a : cu.findAll(AnnotationExpr.class)) {
            String owner = ownerOf(a, selfPackage);
            String name = a.getNameAsString();
            addClassEdge(classEdges, owner, name,
                DependencyEdge.Kind.ANNOTATION, sourceFile,
                a.getBegin().map(p -> p.line).orElse(0),
                a.getBegin().map(p -> p.column).orElse(0),
                "@" + name);
        }

        for (FieldDeclaration fd : cu.findAll(FieldDeclaration.class)) {
            String owner = ownerOf(fd, selfPackage);
            for (var v : fd.getVariables()) {
                String typeName = v.getType().asString();
                String base = baseTypeName(typeName);
                if (base != null) {
                    addClassEdge(classEdges, owner, base,
                        DependencyEdge.Kind.FIELD_TYPE, sourceFile,
                        v.getBegin().map(p -> p.line).orElse(0),
                        v.getBegin().map(p -> p.column).orElse(0),
                        "field " + v.getNameAsString() + " : " + typeName);
                }
            }
        }

        for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
            String owner = ownerOf(m, selfPackage);
            for (Parameter p : m.getParameters()) {
                String base = baseTypeName(p.getTypeAsString());
                if (base != null) {
                    addClassEdge(classEdges, owner, base,
                        DependencyEdge.Kind.PARAM_TYPE, sourceFile,
                        p.getBegin().map(pos -> pos.line).orElse(0),
                        p.getBegin().map(pos -> pos.column).orElse(0),
                        "param " + p.getNameAsString() + " : " + p.getTypeAsString());
                }
            }
            String retType = m.getTypeAsString();
            String retBase = baseTypeName(retType);
            if (retBase != null) {
                addClassEdge(classEdges, owner, retBase,
                    DependencyEdge.Kind.RETURN_TYPE, sourceFile,
                    m.getBegin().map(p -> p.line).orElse(0),
                    m.getBegin().map(p -> p.column).orElse(0),
                    "return " + retType);
            }
        }

        return new DependencyGraph(dedupe(packageEdges), dedupe(classEdges));
    }

    private static void addClassEdge(Map<String, List<DependencyEdge>> map,
                                     String from, String to,
                                     DependencyEdge.Kind kind,
                                     String file, int line, int col, String evidence) {
        if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
            return;
        }
        map.computeIfAbsent(from, k -> new ArrayList<>())
            .add(new DependencyEdge(from, to, kind, file, line, col, evidence));
    }

    private static String baseTypeName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        String t = typeName.trim();
        int lt = t.indexOf('<');
        if (lt >= 0) {
            t = t.substring(0, lt);
        }
        while (t.endsWith("[]")) {
            t = t.substring(0, t.length() - 2);
        }
        int dot = t.lastIndexOf('.');
        String simple = dot >= 0 ? t.substring(dot + 1) : t;
        if (simple.isEmpty()) {
            return null;
        }
        Set<String> primitives = Set.of(
            "int", "long", "short", "byte", "char", "boolean", "float", "double", "void",
            "String", "Object", "Integer", "Long", "Short", "Byte",
            "Character", "Boolean", "Float", "Double",
            "List", "Set", "Map", "Collection", "Iterable", "Optional",
            "ArrayList", "HashMap", "HashSet");
        if (primitives.contains(simple)) {
            return null;
        }
        return simple;
    }

    private static Map<String, List<DependencyEdge>> dedupe(Map<String, List<DependencyEdge>> input) {
        Map<String, List<DependencyEdge>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<DependencyEdge>> e : input.entrySet()) {
            Set<String> seen = new LinkedHashSet<>();
            List<DependencyEdge> list = new ArrayList<>();
            for (DependencyEdge d : e.getValue()) {
                String key = d.from() + "->" + d.to() + ":" + d.kind() + ":" + d.line();
                if (seen.add(key)) {
                    list.add(d);
                }
            }
            out.put(e.getKey(), List.copyOf(list));
        }
        return out;
    }

    @SuppressWarnings("unused")
    private static boolean unused(Node n) {
        return n != null;
    }

    @SuppressWarnings("unused")
    private static Type unusedType(Type t) {
        return t;
    }
}
