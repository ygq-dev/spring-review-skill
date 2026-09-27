package dev.springreview.engine;

import dev.springreview.parser.ParsedBundle;
import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import dev.springreview.tools.IssueCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 配置规则检测器：处理 RULE_ENGINE + category_l1 ∈ {CF, CLOUD, OBS}。
 * 读 parameters.forbidden_keys / required_keys / expected_value。
 * 针对 .yml/.yaml/.properties 文件；按行匹配。
 */
public final class ConfigKeyDetector implements Detector {

    @Override
    public boolean supports(Rule rule) {
        if (!"RULE_ENGINE".equalsIgnoreCase(rule.detectionMethod())
            && !"HYBRID".equalsIgnoreCase(rule.detectionMethod())) {
            return false;
        }
        String l1 = rule.categoryL1();
        return "CF".equals(l1) || "CLOUD".equals(l1) || "OBS".equals(l1);
    }

    @Override
    public List<IssueCandidate> detect(Rule rule, SourceBundle bundle, ParsedBundle parsed) {
        List<IssueCandidate> out = new ArrayList<>();
        List<String> forbiddenKeys = listParam(rule, "forbidden_keys");
        List<String> requiredKeys = listParam(rule, "required_keys");
        String expectedValue = strParam(rule, "expected_value");
        List<String> externalDeps = listParam(rule, "external_dependencies");

        for (SourceUnit u : bundle.units()) {
            String path = u.path();
            if (!isConfigFile(path)) {
                continue;
            }
            String[] lines = u.content().split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String lower = line.toLowerCase().trim();
                int lineNo = i + 1;
                for (String k : forbiddenKeys) {
                    if (lower.contains(k.toLowerCase() + ":")
                        || lower.contains(k.toLowerCase() + "=")) {
                        add(out, rule, u, lineNo, line, "forbidden key: " + k);
                    }
                }
                for (String k : externalDeps) {
                    if (lower.contains("liveness") && lower.contains(k.toLowerCase())) {
                        add(out, rule, u, lineNo, line, "liveness depends on " + k);
                    }
                }
                if (expectedValue != null && !expectedValue.isEmpty()) {
                    for (String k : requiredKeys) {
                        if (lower.contains(k.toLowerCase() + ":")) {
                            int colon = line.indexOf(':');
                            if (colon >= 0 && !line.substring(colon + 1).contains(expectedValue)) {
                                add(out, rule, u, lineNo, line,
                                    "expected value '" + expectedValue + "' for " + k);
                            }
                        }
                    }
                }
            }
            // required_keys 整体缺失 → 单独命中一次
            if (!requiredKeys.isEmpty()) {
                for (String k : requiredKeys) {
                    if (!u.content().toLowerCase().contains(k.toLowerCase())) {
                        // 兜底 evidence：使用“文件中缺少 <key>”
                        add(out, rule, u, 1,
                            "missing key: " + k,
                            "missing required key: " + k);
                    }
                }
            }
        }
        return out;
    }

    private static boolean isConfigFile(String path) {
        String p = path.toLowerCase();
        return p.endsWith(".yml") || p.endsWith(".yaml") || p.endsWith(".properties");
    }

    private static void add(List<IssueCandidate> out, Rule rule, SourceUnit u,
                            int line, String evidence, String note) {
        // evidence 兜底：空时用 note，再空时用 rule.id()
        String ev = (evidence == null || evidence.isBlank()) ? note : evidence.trim();
        if (ev == null || ev.isBlank()) {
            ev = rule.id();
        }
        out.add(new IssueCandidate(
            rule.id(), rule.severity(), rule.confidence(),
            "RULE_ENGINE", u.path(), line, 1,
            ev, MessageTemplates.render(rule.message(), Map.of(
            "file", u.path() == null ? "" : u.path())) + " (" + note + ")",
            rule.remediation(),
            "CUSTOM", rule.toolRuleId(),
            "config:" + note));
    }

    @SuppressWarnings("unchecked")
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
                out.add(String.valueOf(o));
            }
            return out;
        }
        return List.of(String.valueOf(v));
    }

    private static String strParam(Rule rule, String key) {
        if (rule.parameters() == null) {
            return null;
        }
        Object v = rule.parameters().get(key);
        return v == null ? null : String.valueOf(v);
    }
}
