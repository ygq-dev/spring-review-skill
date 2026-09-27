package dev.springreview.engine;

public record Issue(
    String issueId,
    String ruleId,
    String severity,
    String confidence,
    String detectionMethod,
    String file,
    int line,
    int column,
    String evidence,
    String message,
    String remediation,
    String tool,
    String toolRuleId,
    String testCaseId,
    String snippet,
    String fingerprint,
    String issueStatus,
    java.util.List<String> tags
) {
}
