package dev.springreview.rules;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * M07 输出。rules 仅含 ACTIVE 且通过校验的规则，顺序继承索引 items 顺序。
 */
public record RuleSet(
    List<Rule> rules,
    String indexVersion,
    Instant loadedAt,
    String sourceDir,
    List<Diagnostic> diagnostics,
    Map<String, Rule> byId
) {

    public record Diagnostic(
        String file,
        int docIndex,
        String ruleId,
        String level,
        String message
    ) {
    }

    public Rule findById(String id) {
        return byId.get(id);
    }
}
