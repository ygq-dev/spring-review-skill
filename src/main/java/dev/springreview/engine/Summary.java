package dev.springreview.engine;

import java.util.Map;

public record Summary(
    int totalIssues,
    Map<String, Integer> bySeverity,
    Map<String, Integer> byCategory,
    int blocking,
    long durationMs,
    Map<String, Long> tokenUsage
) {
}
