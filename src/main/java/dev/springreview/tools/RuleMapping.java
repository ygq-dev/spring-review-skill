package dev.springreview.tools;

import dev.springreview.rules.Rule;
import dev.springreview.rules.RuleSet;
import dev.springreview.rules.SelectedRules;

import java.util.HashMap;
import java.util.Map;

/**
 * tool + tool_rule_id → Rule 映射；供工具输出适配使用。
 */
public final class RuleMapping {

    private final Map<String, Rule> byToolAndRule = new HashMap<>();

    public RuleMapping(SelectedRules selectedRules) {
        for (Rule r : selectedRules.rules()) {
            if (r.toolRuleId() == null || r.toolRuleId().isBlank()) {
                continue;
            }
            String tool = r.tool() == null ? "" : r.tool().toUpperCase();
            byToolAndRule.put(key(tool, r.toolRuleId()), r);
        }
    }

    public static RuleMapping fromRuleSet(RuleSet ruleSet) {
        SelectedRules all = new SelectedRules(ruleSet.rules(), Map.of(), java.util.List.of());
        return new RuleMapping(all);
    }

    public Rule find(String tool, String toolRuleId) {
        if (tool == null || toolRuleId == null) {
            return null;
        }
        return byToolAndRule.get(key(tool.toUpperCase(), toolRuleId));
    }

    private static String key(String tool, String toolRuleId) {
        return tool + "::" + toolRuleId;
    }
}
