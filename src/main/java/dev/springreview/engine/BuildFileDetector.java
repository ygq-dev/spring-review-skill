package dev.springreview.engine;

import dev.springreview.parser.ParsedBundle;
import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import dev.springreview.tools.IssueCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * BUILD 规则检测器：pom.xml / build.gradle。
 * 读 parameters.pattern（如 "SNAPSHOT"）。
 */
public final class BuildFileDetector implements Detector {

    @Override
    public boolean supports(Rule rule) {
        return "BUILD".equals(rule.categoryL1());
    }

    @Override
    public List<IssueCandidate> detect(Rule rule, SourceBundle bundle, ParsedBundle parsed) {
        List<IssueCandidate> out = new ArrayList<>();
        String pattern = strParam(rule, "pattern");
        for (SourceUnit u : bundle.units()) {
            String path = u.path();
            String lower = path.toLowerCase();
            if (!lower.endsWith("pom.xml")
                && !lower.endsWith("build.gradle")
                && !lower.endsWith("build.gradle.kts")) {
                continue;
            }
            String[] lines = u.content().split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (pattern != null && !pattern.isEmpty()
                    && line.contains(pattern)) {
                    out.add(new IssueCandidate(
                        rule.id(), rule.severity(), rule.confidence(),
                        "REGEX", path, i + 1, 1,
                        line.trim(), rule.message(), rule.remediation(),
                        "CUSTOM", rule.toolRuleId(),
                        "build:" + pattern));
                }
            }
        }
        return out;
    }

    private static String strParam(Rule rule, String key) {
        if (rule.parameters() == null) {
            return null;
        }
        Object v = rule.parameters().get(key);
        return v == null ? null : String.valueOf(v);
    }
}
