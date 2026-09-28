package dev.springreview.engine;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import dev.springreview.parser.ParsedBundle;
import dev.springreview.parser.ParsedSource;
import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceBundle;
import dev.springreview.tools.IssueCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AST 精准检测器，覆盖 15 条高频规则：
 *   - SRS-DI-01-001  字段注入
 *   - SRS-DI-02-001  非 final 依赖字段
 *   - SRS-DAO-01-001 @Transactional private/final
 *   - SRS-DAO-02-001 同类自调用 @Transactional
 *   - SRS-DAO-03-001 只读查询未设 readOnly
 *   - SRS-DAO-04-001 事务内远程调用
 *   - SRS-WEB-01-001 Controller 可变实例字段
 *   - SRS-WEB-02-001 @RequestBody 缺 @Valid
 *   - SRS-WEB-04-001 跨域配置过宽
 *   - SRS-CON-01-001 静态共享 SimpleDateFormat
 *   - SRS-SEC-03-001 SQL 字符串拼接
 *   - SRS-SEC-04-001 关闭 CSRF
 *   - SRS-EXC-01-001 catch 块吞异常
 *   - SRS-RES-01-001 未关闭资源
 *   - SRS-OBS-01-001 日志占位符拼接
 */
public final class AstPreciseDetector implements Detector {

    private static final Set<String> SUPPORTED = Set.of(
        "SRS-DI-01-001", "SRS-DI-02-001",
        "SRS-DAO-01-001", "SRS-DAO-02-001", "SRS-DAO-03-001", "SRS-DAO-04-001",
        "SRS-WEB-01-001", "SRS-WEB-02-001", "SRS-WEB-04-001",
        "SRS-CON-01-001", "SRS-SEC-03-001", "SRS-SEC-04-001",
        "SRS-EXC-01-001", "SRS-RES-01-001", "SRS-OBS-01-001");

    private static final Set<String> RESOURCE_TYPES = Set.of(
        "InputStream", "OutputStream", "Reader", "Writer",
        "FileInputStream", "FileOutputStream", "BufferedReader", "BufferedWriter",
        "Connection", "Statement", "PreparedStatement", "ResultSet",
        "Socket", "ServerSocket");

    @Override
    public boolean supports(Rule rule) {
        return SUPPORTED.contains(rule.id());
    }

    @Override
    public List<IssueCandidate> detect(Rule rule, SourceBundle bundle, ParsedBundle parsed) {
        List<IssueCandidate> out = new ArrayList<>();
        for (ParsedSource ps : parsed.parsedSources()) {
            CompilationUnit cu = ps.compilationUnit();
            if (cu == null) {
                continue;
            }
            switch (rule.id()) {
                case "SRS-DI-01-001" -> detectFieldInjection(rule, ps, cu, out, false);
                case "SRS-DI-02-001" -> detectFieldInjection(rule, ps, cu, out, true);
                case "SRS-DAO-01-001" -> detectTransactionalVisibility(rule, ps, cu, out);
                case "SRS-DAO-02-001" -> detectSelfInvocation(rule, ps, cu, out);
                case "SRS-DAO-03-001" -> detectMissingReadOnly(rule, ps, cu, out);
                case "SRS-DAO-04-001" -> detectRemoteInTransaction(rule, ps, cu, out);
                case "SRS-WEB-01-001" -> detectControllerMutableFields(rule, ps, cu, out);
                case "SRS-WEB-02-001" -> detectMissingValid(rule, ps, cu, out);
                case "SRS-WEB-04-001" -> detectWideCors(rule, ps, cu, out);
                case "SRS-CON-01-001" -> detectStaticSimpleDateFormat(rule, ps, cu, out);
                case "SRS-SEC-03-001" -> detectSqlConcatenation(rule, ps, cu, out);
                case "SRS-SEC-04-001" -> detectCsrfDisabled(rule, ps, cu, out);
                case "SRS-EXC-01-001" -> detectEmptyCatch(rule, ps, cu, out);
                case "SRS-RES-01-001" -> detectUnclosedResource(rule, ps, cu, out);
                case "SRS-OBS-01-001" -> detectLogConcat(rule, ps, cu, out);
                default -> { }
            }
        }
        return out;
    }

    // ---------- DI-01 / DI-02 ----------

    private static void detectFieldInjection(Rule rule, ParsedSource ps,
                                             CompilationUnit cu,
                                             List<IssueCandidate> out,
                                             boolean nonFinalOnly) {
        for (FieldDeclaration f : cu.findAll(FieldDeclaration.class)) {
            boolean injected = hasAnyAnnotation(f, "Autowired", "Inject", "Resource");
            if (!injected) {
                continue;
            }
            if (nonFinalOnly && f.isFinal()) {
                continue;
            }
            String varName = f.getVariables().isEmpty() ? "?"
                : f.getVariable(0).getNameAsString();
            String evidence = nonFinalOnly
                ? "non-final field " + varName
                : "field injection on " + varName;
            addHit(out, rule, ps.path(),
                f.getBegin().map(p -> p.line).orElse(0),
                f.getBegin().map(p -> p.column).orElse(0),
                evidence);
        }
    }

    // ---------- DAO-01 ----------

    private static void detectTransactionalVisibility(Rule rule, ParsedSource ps,
                                                      CompilationUnit cu,
                                                      List<IssueCandidate> out) {
        for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
            if (!hasAnyAnnotation(m, "Transactional")) {
                continue;
            }
            if (m.isPrivate() || m.isFinal()) {
                String mod = m.isPrivate() ? "private" : "final";
                addHit(out, rule, ps.path(),
                    m.getBegin().map(p -> p.line).orElse(0),
                    m.getBegin().map(p -> p.column).orElse(0),
                    "@Transactional " + mod + " method " + m.getNameAsString());
            }
        }
    }

    // ---------- DAO-02 ----------

    private static void detectSelfInvocation(Rule rule, ParsedSource ps,
                                             CompilationUnit cu,
                                             List<IssueCandidate> out) {
        Set<String> txMethods = new java.util.HashSet<>();
        for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
            if (hasAnyAnnotation(m, "Transactional")) {
                txMethods.add(m.getNameAsString());
            }
        }
        if (txMethods.isEmpty()) {
            return;
        }
        for (MethodDeclaration outer : cu.findAll(MethodDeclaration.class)) {
            for (MethodCallExpr call : outer.findAll(MethodCallExpr.class)) {
                String callee = call.getNameAsString();
                if (!txMethods.contains(callee)) {
                    continue;
                }
                String scope = call.getScope().map(Object::toString).orElse("");
                if (!"this".equals(scope) && !scope.isEmpty()) {
                    continue;
                }
                if (hasAnyAnnotation(outer, "Transactional")) {
                    continue;
                }
                addHit(out, rule, ps.path(),
                    call.getBegin().map(p -> p.line).orElse(0),
                    call.getBegin().map(p -> p.column).orElse(0),
                    "self-invocation of @Transactional method " + callee);
            }
        }
    }

    // ---------- DAO-03 ----------

    private static void detectMissingReadOnly(Rule rule, ParsedSource ps,
                                              CompilationUnit cu,
                                              List<IssueCandidate> out) {
        for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
            AnnotationExpr tx = findAnnotation(m, "Transactional");
            if (tx == null) {
                continue;
            }
            if (hasReadOnlyTrue(tx)) {
                continue;
            }
            String name = m.getNameAsString();
            if (!looksLikeQuery(name)) {
                continue;
            }
            addHit(out, rule, ps.path(),
                m.getBegin().map(p -> p.line).orElse(0),
                m.getBegin().map(p -> p.column).orElse(0),
                "read-only query " + name + " missing readOnly=true");
        }
    }

    private static boolean looksLikeQuery(String name) {
        String n = name.toLowerCase();
        return n.startsWith("find") || n.startsWith("get") || n.startsWith("query")
            || n.startsWith("list") || n.startsWith("load") || n.startsWith("select")
            || n.startsWith("count") || n.startsWith("exists") || n.startsWith("search")
            || n.startsWith("read");
    }

    private static boolean hasReadOnlyTrue(AnnotationExpr ann) {
        if (!(ann instanceof NormalAnnotationExpr na)) {
            return false;
        }
        return na.getPairs().stream().anyMatch(p ->
            "readOnly".equals(p.getNameAsString())
                && "true".equalsIgnoreCase(p.getValue().toString()));
    }

    // ---------- DAO-04：事务内远程调用 ----------

    private static final Set<String> REMOTE_CALLEES = Set.of(
        "getForObject", "getForEntity", "postForObject", "postForEntity",
        "exchange", "execute", "retrieve", "get", "post", "put", "delete");

    private static void detectRemoteInTransaction(Rule rule, ParsedSource ps,
                                                  CompilationUnit cu,
                                                  List<IssueCandidate> out) {
        for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
            if (!hasAnyAnnotation(m, "Transactional")) {
                continue;
            }
            for (MethodCallExpr call : m.findAll(MethodCallExpr.class)) {
                String scope = call.getScope().map(Object::toString).orElse("");
                if (scope.contains("restTemplate")
                    || scope.contains("webClient")
                    || scope.contains("RestTemplate")
                    || scope.contains("WebClient")
                    || scope.contains("feignClient")
                    || scope.contains("httpClient")) {
                    addHit(out, rule, ps.path(),
                        call.getBegin().map(p -> p.line).orElse(0),
                        call.getBegin().map(p -> p.column).orElse(0),
                        "remote call in @Transactional: " + truncate(call.toString(), 100));
                }
            }
        }
    }

    // ---------- WEB-01 ----------

    private static void detectControllerMutableFields(Rule rule, ParsedSource ps,
                                                      CompilationUnit cu,
                                                      List<IssueCandidate> out) {
        for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (!hasAnyAnnotation(cls, "Controller", "RestController")) {
                continue;
            }
            for (FieldDeclaration f : cls.getFields()) {
                if (f.isStatic() || f.isFinal()) {
                    continue;
                }
                String varName = f.getVariables().isEmpty() ? "?"
                    : f.getVariable(0).getNameAsString();
                addHit(out, rule, ps.path(),
                    f.getBegin().map(p -> p.line).orElse(0),
                    f.getBegin().map(p -> p.column).orElse(0),
                    "mutable field " + varName + " in " + cls.getNameAsString());
            }
        }
    }

    // ---------- WEB-02 ----------

    private static void detectMissingValid(Rule rule, ParsedSource ps,
                                           CompilationUnit cu,
                                           List<IssueCandidate> out) {
        for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
            if (!hasAnyAnnotation(m, "PostMapping", "PutMapping", "PatchMapping", "RequestMapping")) {
                continue;
            }
            for (Parameter p : m.getParameters()) {
                if (!hasAnyAnnotation(p, "RequestBody")) {
                    continue;
                }
                if (hasAnyAnnotation(p, "Valid", "Validated")) {
                    continue;
                }
                addHit(out, rule, ps.path(),
                    p.getBegin().map(pos -> pos.line).orElse(0),
                    p.getBegin().map(pos -> pos.column).orElse(0),
                    "@RequestBody param " + p.getNameAsString() + " missing @Valid");
            }
        }
    }

    // ---------- WEB-04：跨域过宽 ----------

    private static void detectWideCors(Rule rule, ParsedSource ps,
                                       CompilationUnit cu,
                                       List<IssueCandidate> out) {
        boolean hasWildcard = false;
        boolean hasCredentials = false;
        MethodCallExpr firstWildcard = null;
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            String name = call.getNameAsString();
            if ("allowedOrigins".equals(name) && anyArgIsWildcard(call)) {
                hasWildcard = true;
                if (firstWildcard == null) {
                    firstWildcard = call;
                }
            }
            if ("allowCredentials".equals(name)
                && call.getArguments().size() == 1
                && "true".equalsIgnoreCase(call.getArgument(0).toString())) {
                hasCredentials = true;
            }
        }
        if (hasWildcard && hasCredentials && firstWildcard != null) {
            addHit(out, rule, ps.path(),
                firstWildcard.getBegin().map(p -> p.line).orElse(0),
                firstWildcard.getBegin().map(p -> p.column).orElse(0),
                "allowedOrigins(*) with allowCredentials(true)");
        }
    }

    private static boolean anyArgIsWildcard(MethodCallExpr call) {
        for (Expression arg : call.getArguments()) {
            String s = arg.toString();
            if (s.contains("\"*\"")) {
                return true;
            }
        }
        return false;
    }

    // ---------- CON-01 ----------

    private static void detectStaticSimpleDateFormat(Rule rule, ParsedSource ps,
                                                     CompilationUnit cu,
                                                     List<IssueCandidate> out) {
        for (FieldDeclaration f : cu.findAll(FieldDeclaration.class)) {
            if (!f.isStatic()) {
                continue;
            }
            String typeName = fieldTypeName(f);
            if (typeName == null) {
                continue;
            }
            if (typeName.equals("SimpleDateFormat") || typeName.endsWith(".SimpleDateFormat")) {
                String varName = f.getVariables().isEmpty() ? "?"
                    : f.getVariable(0).getNameAsString();
                addHit(out, rule, ps.path(),
                    f.getBegin().map(p -> p.line).orElse(0),
                    f.getBegin().map(p -> p.column).orElse(0),
                    "static SimpleDateFormat field " + varName);
            }
        }
    }

    private static String fieldTypeName(FieldDeclaration f) {
        if (f.getVariables().isEmpty()) {
            return null;
        }
        return f.getVariable(0).getType().asString();
    }

    // ---------- SEC-03 ----------

    private static void detectSqlConcatenation(Rule rule, ParsedSource ps,
                                               CompilationUnit cu,
                                               List<IssueCandidate> out) {
        for (BinaryExpr be : cu.findAll(BinaryExpr.class)) {
            if (be.getOperator() != BinaryExpr.Operator.PLUS) {
                continue;
            }
            if (!containsSqlLiteral(be)) {
                continue;
            }
            if (!containsNonLiteralOperand(be)) {
                continue;
            }
            addHit(out, rule, ps.path(),
                be.getBegin().map(p -> p.line).orElse(0),
                be.getBegin().map(p -> p.column).orElse(0),
                "SQL string concatenation: " + truncate(be.toString(), 120));
        }
    }

    private static boolean containsSqlLiteral(Expression e) {
        if (e instanceof StringLiteralExpr s) {
            String v = s.asString().trim().toUpperCase();
            return v.startsWith("SELECT") || v.startsWith("INSERT")
                || v.startsWith("UPDATE") || v.startsWith("DELETE")
                || v.contains(" FROM ") || v.contains(" WHERE ");
        }
        if (e instanceof BinaryExpr b) {
            return containsSqlLiteral(b.getLeft()) || containsSqlLiteral(b.getRight());
        }
        if (e instanceof com.github.javaparser.ast.expr.EnclosedExpr en) {
            return containsSqlLiteral(en.getInner());
        }
        return false;
    }

    private static boolean containsNonLiteralOperand(Expression e) {
        if (e instanceof StringLiteralExpr) {
            return false;
        }
        if (e instanceof BinaryExpr b) {
            return containsNonLiteralOperand(b.getLeft())
                || containsNonLiteralOperand(b.getRight());
        }
        if (e instanceof com.github.javaparser.ast.expr.EnclosedExpr en) {
            return containsNonLiteralOperand(en.getInner());
        }
        return true;
    }

    // ---------- SEC-04：CSRF 关闭 ----------

    private static void detectCsrfDisabled(Rule rule, ParsedSource ps,
                                           CompilationUnit cu,
                                           List<IssueCandidate> out) {
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            if (!"disable".equals(call.getNameAsString())) {
                continue;
            }
            String scope = call.getScope().map(Object::toString).orElse("");
            if (scope.contains("csrf") || scope.contains("Csrf")) {
                addHit(out, rule, ps.path(),
                    call.getBegin().map(p -> p.line).orElse(0),
                    call.getBegin().map(p -> p.column).orElse(0),
                    "csrf disabled: " + truncate(call.toString(), 80));
            }
        }
    }

    // ---------- EXC-01 ----------

    private static void detectEmptyCatch(Rule rule, ParsedSource ps,
                                         CompilationUnit cu,
                                         List<IssueCandidate> out) {
        for (CatchClause cc : cu.findAll(CatchClause.class)) {
            com.github.javaparser.ast.stmt.BlockStmt body = cc.getBody();
            if (body.getStatements().isEmpty()) {
                boolean documented = !body.getAllContainedComments().isEmpty()
                    || !body.getOrphanComments().isEmpty();
                int lineNo = cc.getBegin().map(p -> p.line).orElse(0);
                int colNo = cc.getBegin().map(p -> p.column).orElse(0);
                if (documented) {
                    // 有注释说明意图：按规则规格仍是"无有效处理逻辑"，但降置信度交人工复核
                    String evidence = "commented empty catch: " + cc.getParameter().getTypeAsString();
                    out.add(new IssueCandidate(
                        rule.id(), rule.severity(), "MEDIUM",
                        "AST", ps.path(), Math.max(1, lineNo), Math.max(1, colNo),
                        evidence,
                        MessageTemplates.render(rule.message(), Map.of("file", ps.path()))
                            + "（catch 块含注释说明意图，建议人工确认）",
                        rule.remediation(),
                        "CUSTOM", rule.toolRuleId(),
                        "ast:" + evidence));
                } else {
                    addHit(out, rule, ps.path(), lineNo, colNo,
                        "empty catch: " + cc.getParameter().getTypeAsString());
                }
            }
        }
    }

    // ---------- RES-01：未关闭资源 ----------

    private static void detectUnclosedResource(Rule rule, ParsedSource ps,
                                               CompilationUnit cu,
                                               List<IssueCandidate> out) {
        // 收集 try-with-resources 中的变量名
        Set<String> wrapped = new java.util.HashSet<>();
        for (TryStmt ts : cu.findAll(TryStmt.class)) {
            ts.getResources().forEach(r -> {
                if (r.isVariableDeclarationExpr()) {
                    for (VariableDeclarator v : r.asVariableDeclarationExpr().getVariables()) {
                        wrapped.add(v.getNameAsString());
                    }
                }
            });
        }

        // 找局部变量声明为资源类型
        for (var decl : cu.findAll(com.github.javaparser.ast.expr.VariableDeclarationExpr.class)) {
            for (VariableDeclarator v : decl.getVariables()) {
                String type = v.getType().asString();
                String simple = simpleName(type);
                if (!RESOURCE_TYPES.contains(simple)) {
                    continue;
                }
                String varName = v.getNameAsString();
                if (wrapped.contains(varName)) {
                    continue;
                }
                // 检查是否有 .close() 调用（该变量作为 scope 或参数）
                boolean hasClose = false;
                for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                    if (!"close".equals(call.getNameAsString())) {
                        continue;
                    }
                    String scope = call.getScope().map(Object::toString).orElse("");
                    if (scope.equals(varName)) {
                        hasClose = true;
                        break;
                    }
                    for (Expression arg : call.getArguments()) {
                        if (arg.toString().equals(varName)) {
                            hasClose = true;
                            break;
                        }
                    }
                    if (hasClose) {
                        break;
                    }
                }
                if (!hasClose) {
                    addHit(out, rule, ps.path(),
                        v.getBegin().map(p -> p.line).orElse(0),
                        v.getBegin().map(p -> p.column).orElse(0),
                        "resource " + varName + " (" + simple + ") not closed");
                }
            }
        }
    }

    private static String simpleName(String type) {
        if (type == null) {
            return "";
        }
        String t = type.trim();
        int lt = t.indexOf('<');
        if (lt >= 0) {
            t = t.substring(0, lt);
        }
        while (t.endsWith("[]")) {
            t = t.substring(0, t.length() - 2);
        }
        int dot = t.lastIndexOf('.');
        return dot >= 0 ? t.substring(dot + 1) : t;
    }

    // ---------- OBS-01：日志拼接 ----------

    private static void detectLogConcat(Rule rule, ParsedSource ps,
                                        CompilationUnit cu,
                                        List<IssueCandidate> out) {
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            String name = call.getNameAsString();
            if (!("trace".equals(name) || "debug".equals(name)
                || "info".equals(name) || "warn".equals(name) || "error".equals(name))) {
                continue;
            }
            String scope = call.getScope().map(Object::toString).orElse("");
            if (!(scope.equals("log") || scope.equals("logger")
                || scope.endsWith(".log") || scope.endsWith(".logger"))) {
                continue;
            }
            if (call.getArguments().isEmpty()) {
                continue;
            }
            Expression first = call.getArgument(0);
            if (!(first instanceof BinaryExpr be)) {
                continue;
            }
            if (be.getOperator() != BinaryExpr.Operator.PLUS) {
                continue;
            }
            // 要求是字符串拼接
            if (!containsStringLiteral(be)) {
                continue;
            }
            addHit(out, rule, ps.path(),
                call.getBegin().map(p -> p.line).orElse(0),
                call.getBegin().map(p -> p.column).orElse(0),
                "log concat: " + truncate(call.toString(), 100));
        }
    }

    private static boolean containsStringLiteral(Expression e) {
        if (e instanceof StringLiteralExpr) {
            return true;
        }
        if (e instanceof BinaryExpr b) {
            return containsStringLiteral(b.getLeft()) || containsStringLiteral(b.getRight());
        }
        if (e instanceof com.github.javaparser.ast.expr.EnclosedExpr en) {
            return containsStringLiteral(en.getInner());
        }
        return false;
    }

    // ---------- helpers ----------

    private static void addHit(List<IssueCandidate> out, Rule rule, String file,
                               int line, int col, String evidence) {
        out.add(new IssueCandidate(
            rule.id(), rule.severity(), rule.confidence(),
            "AST", file, Math.max(1, line), Math.max(1, col),
            evidence,
            // 纵深防御：即便未来 AstPrecise 规则引入占位符也不会泄漏原始 token
            MessageTemplates.render(rule.message(), Map.of("file", file)),
            rule.remediation(),
            "CUSTOM", rule.toolRuleId(),
            "ast:" + evidence));
    }

    private static boolean hasAnyAnnotation(NodeWithAnnotations<?> n, String... names) {
        for (AnnotationExpr a : n.getAnnotations()) {
            String simple = a.getNameAsString();
            int dot = simple.lastIndexOf('.');
            String simpleName = dot >= 0 ? simple.substring(dot + 1) : simple;
            for (String want : names) {
                if (want.equals(simpleName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static AnnotationExpr findAnnotation(NodeWithAnnotations<?> n, String name) {
        for (AnnotationExpr a : n.getAnnotations()) {
            String simple = a.getNameAsString();
            int dot = simple.lastIndexOf('.');
            String simpleName = dot >= 0 ? simple.substring(dot + 1) : simple;
            if (name.equals(simpleName)) {
                return a;
            }
        }
        return null;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    @SuppressWarnings("unused")
    private static Object unused(ObjectCreationExpr o, Statement s) {
        return o == null && s == null ? null : null;
    }
}
