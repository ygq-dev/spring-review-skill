package dev.springreview.tools;

import java.util.Objects;

/**
 * 内部候选问题；不直接进 A6 报告。
 * M13 聚合后才会成为最终 Issue。
 */
public final class IssueCandidate {

    private final String ruleId;
    private final String severity;
    private final String confidence;
    private final String detectionMethod;
    private final String file;
    private final int line;
    private final int column;
    private final String evidence;
    private final String message;
    private final String remediation;
    private final String sourceTool;
    private final String toolRuleId;
    private final String rawEvidence;

    public IssueCandidate(String ruleId, String severity, String confidence,
                          String detectionMethod, String file, int line, int column,
                          String evidence, String message, String remediation,
                          String sourceTool, String toolRuleId, String rawEvidence) {
        this.ruleId = ruleId;
        this.severity = severity;
        this.confidence = confidence;
        this.detectionMethod = detectionMethod;
        this.file = file;
        this.line = Math.max(0, line);
        this.column = Math.max(0, column);
        this.evidence = evidence == null ? "" : evidence;
        this.message = message == null ? "" : message;
        this.remediation = remediation == null ? "" : remediation;
        this.sourceTool = sourceTool;
        this.toolRuleId = toolRuleId;
        this.rawEvidence = rawEvidence;
    }

    public String ruleId() { return ruleId; }
    public String severity() { return severity; }
    public String confidence() { return confidence; }
    public String detectionMethod() { return detectionMethod; }
    public String file() { return file; }
    public int line() { return line; }
    public int column() { return column; }
    public String evidence() { return evidence; }
    public String message() { return message; }
    public String remediation() { return remediation; }
    public String sourceTool() { return sourceTool; }
    public String toolRuleId() { return toolRuleId; }
    public String rawEvidence() { return rawEvidence; }

    public String fingerprint() {
        String seed = String.join("|",
            sourceTool == null ? "" : sourceTool,
            toolRuleId == null ? "" : toolRuleId,
            ruleId == null ? "" : ruleId,
            file == null ? "" : file,
            String.valueOf(line),
            String.valueOf(column),
            message == null ? "" : message.trim());
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(out);
        } catch (java.security.NoSuchAlgorithmException e) {
            return Integer.toHexString(seed.hashCode());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IssueCandidate that)) {
            return false;
        }
        return line == that.line && column == that.column
            && Objects.equals(ruleId, that.ruleId)
            && Objects.equals(file, that.file)
            && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, file, line, column, message);
    }
}
