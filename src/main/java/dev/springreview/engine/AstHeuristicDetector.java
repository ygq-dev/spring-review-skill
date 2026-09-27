package dev.springreview.engine;

import dev.springreview.parser.ParsedBundle;
import dev.springreview.parser.ParsedSource;
import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceBundle;
import dev.springreview.tools.IssueCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * AST 规则的通用回退检测器。
 * 通过注解名、类名做启发式匹配；已限流避免单规则单文件爆炸。
 * 专用检测器存在时优先（RuleEngine 顺序保证）。
 */
public final class AstHeuristicDetector implements Detector {

    private static final int MAX_PER_FILE_PER_RULE = 5;

    @Override
    public boolean supports(Rule rule) {
        if (!"AST".equalsIgnoreCase(rule.detectionMethod())) {
            return false;
        }
        return !"AR".equals(rule.categoryL1());
    }

    @Override
    public List<IssueCandidate> detect(Rule rule, SourceBundle bundle, ParsedBundle parsed) {
        List<IssueCandidate> out = new ArrayList<>();
        List<String> annotations = listParam(rule, "annotations");
        if (annotations.isEmpty()) {
            return out;
        }
        for (ParsedSource ps : parsed.parsedSources()) {
            int perFileHits = 0;
            for (ParsedSource.TypeInfo t : ps.types()) {
                if (perFileHits >= MAX_PER_FILE_PER_RULE) {
                    break;
                }
                for (String a : t.annotations()) {
                    if (perFileHits >= MAX_PER_FILE_PER_RULE) {
                        break;
                    }
                    if (annotations.contains(a) || annotations.contains("@" + a)) {
                        out.add(new IssueCandidate(
                            rule.id(), rule.severity(), rule.confidence(),
                            "AST", ps.path(),
                            t.beginLine(), 1,
                            "@" + a + " on " + t.simpleName(),
                            rule.message(), rule.remediation(),
                            "CUSTOM", rule.toolRuleId(),
                            "ast.annotation:" + a));
                        perFileHits++;
                    }
                }
            }
        }
        return out;
    }

    private static List<String> listParam(Rule rule, String key) {
        if (rule.parameters() == null) {
            return List.of();
        }
        Object v = rule.parameters().get(key);
        if (v == null) {
            return List.of();
        }
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                out.add(String.valueOf(o).replace("@", ""));
            }
            return out;
        }
        return List.of(String.valueOf(v).replace("@", ""));
    }
}
