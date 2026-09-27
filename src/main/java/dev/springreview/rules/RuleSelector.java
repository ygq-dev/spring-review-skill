package dev.springreview.rules;

import dev.springreview.config.AppConfig;
import dev.springreview.observability.Logs;
import dev.springreview.scope.ReviewScope;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * M08：按 scope.mode、applies_to、enabled、tool、detection_method 过滤规则。
 * F4 首发版：enabled + exclude + module_type（粗粒度）+ status（M07 已过滤）。
 * applies_to 版本表达式、文件类型、tool/detection 精确过滤留到 F4.1。
 */
public final class RuleSelector {

    private static final Logger LOG = Logs.logger("rule-selector");

    public SelectedRules select(RuleSet ruleSet, ReviewScope scope, AppConfig config) {
        Logs.module("M08");
        List<Rule> selected = new ArrayList<>();
        Map<String, String> reasons = new LinkedHashMap<>();

        for (Rule rule : ruleSet.rules()) {
            String reason = evaluate(rule, scope, config);
            reasons.put(rule.id(), reason);
            if ("SELECTED".equals(reason)) {
                selected.add(rule);
            }
        }
        LOG.info("rules selected {}/{}", selected.size(), ruleSet.rules().size());
        return new SelectedRules(List.copyOf(selected), Map.copyOf(reasons), List.of());
    }

    private static String evaluate(Rule rule, ReviewScope scope, AppConfig config) {
        if (!rule.enabled()) {
            return "DISABLED";
        }
        Rule.AppliesTo at = rule.appliesTo();
        if (at != null) {
            if (!at.buildTool().isEmpty()
                && !at.buildTool().contains("all")
                && scope.repoRoot() == null) {
                // build_tool 检测需要读取 pom.xml / build.gradle，F4.1 补
                // 当前放行
            }
            if (matchesExclude(at, scope)) {
                return "EXCLUDED";
            }
        }
        return "SELECTED";
    }

    private static boolean matchesExclude(Rule.AppliesTo at, ReviewScope scope) {
        if (at.exclude().isEmpty()) {
            return false;
        }
        // F4 首发版：只有在 review scope 提供了 filePaths 时才做排除判断
        if (scope.filePaths() == null || scope.filePaths().isEmpty()) {
            return false;
        }
        for (String f : scope.filePaths()) {
            for (String ex : at.exclude()) {
                if (globMatches(ex, f)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean globMatches(String glob, String path) {
        String normalized = path.replace('\\', '/');
        String g = glob.replace('\\', '/');
        if (g.startsWith("**/") && g.endsWith("/**")) {
            String mid = g.substring(3, g.length() - 3);
            return normalized.contains("/" + mid + "/") || normalized.startsWith(mid + "/");
        }
        if (g.startsWith("**/")) {
            String tail = g.substring(3);
            return normalized.endsWith(tail) || normalized.contains("/" + tail);
        }
        if (g.endsWith("/**")) {
            String head = g.substring(0, g.length() - 3);
            return normalized.startsWith(head + "/") || normalized.equals(head);
        }
        return normalized.equals(g);
    }
}
