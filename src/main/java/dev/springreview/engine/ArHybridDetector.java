package dev.springreview.engine;

import dev.springreview.parser.DependencyEdge;
import dev.springreview.parser.DependencyGraph;
import dev.springreview.parser.ParsedBundle;
import dev.springreview.parser.ParsedSource;
import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceBundle;
import dev.springreview.tools.IssueCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AR 规则专用检测器：基于 M09 依赖图。
 * - SRS-AR-01-001：Controller 直接依赖 Repository/Mapper
 * - SRS-AR-02-001：domain 包依赖 web/jdbc/mq 等
 */
public final class ArHybridDetector implements Detector {

    @Override
    public boolean supports(Rule rule) {
        if (!"AR".equals(rule.categoryL1())) {
            return false;
        }
        return "SRS-AR-01-001".equals(rule.id())
            || "SRS-AR-02-001".equals(rule.id());
    }

    @Override
    public List<IssueCandidate> detect(Rule rule, SourceBundle bundle, ParsedBundle parsed) {
        if ("SRS-AR-01-001".equals(rule.id())) {
            return detectAr01(rule, parsed);
        }
        if ("SRS-AR-02-001".equals(rule.id())) {
            return detectAr02(rule, parsed);
        }
        return List.of();
    }

    /** Controller → Repository/Mapper 直接依赖 */
    private List<IssueCandidate> detectAr01(Rule rule, ParsedBundle parsed) {
        List<IssueCandidate> out = new ArrayList<>();
        for (ParsedSource ps : parsed.parsedSources()) {
            String typeName = firstTypeName(ps);
            if (typeName == null || !isController(ps)) {
                continue;
            }
            DependencyGraph g = ps.dependencyGraph();
            List<DependencyEdge> edges = g.classEdges().get(typeName);
            if (edges == null) {
                continue;
            }
            for (DependencyEdge e : edges) {
                if (isRepositoryTarget(e.to())) {
                    out.add(new IssueCandidate(
                        rule.id(), rule.severity(), rule.confidence(),
                        "HYBRID", ps.path(), e.line(), e.column(),
                        e.evidence(),
                        MessageTemplates.render(rule.message(), Map.of(
                            "className", simpleName(typeName),
                            "repositoryName", simpleName(e.to()),
                            "file", ps.path())),
                        rule.remediation(),
                        "CUSTOM", null,
                        "ar01:" + e.kind() + ":" + e.to()));
                }
            }
        }
        return out;
    }

    /** domain → web/jdbc/mq */
    private List<IssueCandidate> detectAr02(Rule rule, ParsedBundle parsed) {
        List<IssueCandidate> out = new ArrayList<>();
        for (ParsedSource ps : parsed.parsedSources()) {
            String pkg = ps.packageName() == null ? "" : ps.packageName().toLowerCase();
            if (!pkg.contains(".domain") && !pkg.contains(".model")
                && !pkg.endsWith("domain") && !pkg.endsWith("model")) {
                continue;
            }
            String typeName = firstTypeName(ps);
            if (typeName == null) {
                continue;
            }
            List<DependencyEdge> edges = ps.dependencyGraph().classEdges().get(typeName);
            if (edges == null) {
                continue;
            }
            for (DependencyEdge e : edges) {
                if (isInfraTarget(e.to()) || isWebAnnotationTarget(e.to())) {
                    out.add(new IssueCandidate(
                        rule.id(), rule.severity(), rule.confidence(),
                        "HYBRID", ps.path(), e.line(), e.column(),
                        e.evidence(),
                        MessageTemplates.render(rule.message(), Map.of(
                            "className", simpleName(typeName),
                            "targetPackage", simpleName(e.to()),
                            "file", ps.path())),
                        rule.remediation(),
                        "CUSTOM", null,
                        "ar02:" + e.kind() + ":" + e.to()));
                }
            }
        }
        return out;
    }

    private static String firstTypeName(ParsedSource ps) {
        if (ps.types().isEmpty()) {
            return null;
        }
        return ps.types().get(0).qualifiedName();
    }

    private static String simpleName(String qualified) {
        if (qualified == null) {
            return "";
        }
        return qualified.contains(".") ? qualified.substring(qualified.lastIndexOf('.') + 1) : qualified;
    }

    private static boolean isController(ParsedSource ps) {
        for (ParsedSource.TypeInfo t : ps.types()) {
            for (String a : t.annotations()) {
                if ("RestController".equals(a) || "Controller".equals(a)) {
                    return true;
                }
            }
            if (t.simpleName().endsWith("Controller")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRepositoryTarget(String name) {
        if (name == null) {
            return false;
        }
        String simple = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
        return simple.endsWith("Repository")
            || simple.endsWith("Mapper")
            || simple.endsWith("Dao")
            || simple.endsWith("DAO");
    }

    private static boolean isInfraTarget(String name) {
        if (name == null) {
            return false;
        }
        String simple = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
        String lower = simple.toLowerCase();
        return lower.endsWith("repository")
            || lower.endsWith("jdbc")
            || lower.endsWith("mapper")
            || lower.endsWith("kafka")
            || lower.endsWith("redis")
            || lower.endsWith("mq")
            || lower.endsWith("client")
            || lower.endsWith("producer")
            || lower.endsWith("consumer");
    }

    private static boolean isWebAnnotationTarget(String name) {
        if (name == null) {
            return false;
        }
        return switch (name) {
            case "RestController", "Controller", "RequestMapping",
                "GetMapping", "PostMapping", "PutMapping", "DeleteMapping" -> true;
            default -> false;
        };
    }
}
