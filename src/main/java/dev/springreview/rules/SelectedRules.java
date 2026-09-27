package dev.springreview.rules;

import java.util.List;
import java.util.Map;

/**
 * M08 输出。
 */
public record SelectedRules(
    List<Rule> rules,
    Map<String, String> selectionReason,
    List<String> skipped
) {
}
