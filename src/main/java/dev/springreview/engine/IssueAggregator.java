package dev.springreview.engine;

import dev.springreview.observability.Logs;
import dev.springreview.rules.Rule;
import dev.springreview.rules.RuleSet;
import dev.springreview.tools.IssueCandidate;
import dev.springreview.tools.ToolRun;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * M13：聚合、去重、排序、Summary。
 */
public final class IssueAggregator {

    private static final Logger LOG = Logs.logger("aggregator");

    private static final Map<String, Integer> SEV_ORDER = Map.of(
        "BLOCKER", 0, "CRITICAL", 1, "MAJOR", 2, "MINOR", 3, "INFO", 4);

    public AggregatedResult aggregate(List<IssueCandidate> candidates,
                                      RuleSet ruleSet,
                                      List<ToolRun> toolRuns,
                                      long durationMs,
                                      Map<String, Long> tokenUsage) {
        Logs.module("M13");
        Map<String, Issue> dedup = new LinkedHashMap<>();
        for (IssueCandidate c : candidates) {
            Issue issue = toIssue(c);
            if (issue == null) {
                continue;
            }
            String key = issue.ruleId() + "|" + issue.file() + "|"
                + issue.line() + "|" + normalizedMsg(issue.message());
            Issue existing = dedup.get(key);
            if (existing == null || rank(issue.confidence()) > rank(existing.confidence())) {
                dedup.put(key, issue);
            }
        }
        List<Issue> unique = new ArrayList<>(dedup.values());
        unique.sort(issueComparator());

        Map<String, Integer> bySeverity = new TreeMap<>();
        for (String s : SEV_ORDER.keySet()) {
            bySeverity.put(s, 0);
        }
        Map<String, Integer> byCategory = new LinkedHashMap<>();
        int blocking = 0;
        for (Issue i : unique) {
            bySeverity.merge(i.severity(), 1, Integer::sum);
            String cat = categoryOf(i.ruleId(), ruleSet);
            byCategory.merge(cat, 1, Integer::sum);
            if ("BLOCKER".equals(i.severity()) || "CRITICAL".equals(i.severity())) {
                blocking++;
            }
        }
        Summary summary = new Summary(
            unique.size(), bySeverity, byCategory, blocking, durationMs, tokenUsage);

        LOG.info("aggregated issues={} blocking={} bySeverity={}",
            unique.size(), blocking, bySeverity);
        return new AggregatedResult(List.copyOf(unique), summary, toolRuns);
    }

    private static Issue toIssue(IssueCandidate c) {
        if (c.ruleId() == null || c.file() == null) {
            return null;
        }
        int line = Math.max(1, c.line());
        int col = Math.max(1, c.column());
        String fingerprint = c.fingerprint();

        // evidence 兜底（A6 minLength: 1）
        String evidence = c.evidence();
        if (evidence == null || evidence.isBlank()) {
            evidence = c.message();
        }
        if (evidence == null || evidence.isBlank()) {
            evidence = c.ruleId();
        }
        if (evidence == null || evidence.isBlank()) {
            evidence = "<no evidence>";
        }

        // message 兜底
        String message = c.message();
        if (message == null || message.isBlank()) {
            message = c.ruleId();
        }

        // remediation 兜底
        String remediation = c.remediation();
        if (remediation == null || remediation.isBlank()) {
            remediation = "按规则 " + c.ruleId() + " 处理。";
        }

        String issueId = "ISSUE-" + sha8(fingerprint);
        return new Issue(
            issueId,
            c.ruleId(),
            blankToDefault(c.severity(), "MAJOR"),
            blankToDefault(c.confidence(), "MEDIUM"),
            blankToDefault(c.detectionMethod(), "STATIC_ANALYSIS"),
            c.file(),
            line, col,
            evidence,
            message,
            remediation,
            c.sourceTool(),
            c.toolRuleId(),
            null,
            evidence,
            fingerprint,
            "OPEN",
            List.of()
        );
    }

    private static Comparator<Issue> issueComparator() {
        return Comparator
            .comparingInt((Issue i) -> SEV_ORDER.getOrDefault(i.severity(), 9))
            .thenComparingInt(i -> -rank(i.confidence()))
            .thenComparing(Issue::file)
            .thenComparingInt(Issue::line)
            .thenComparingInt(Issue::column)
            .thenComparing(Issue::ruleId);
    }

    private static int rank(String confidence) {
        if (confidence == null) {
            return 0;
        }
        return switch (confidence) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private static String categoryOf(String ruleId, RuleSet ruleSet) {
        if (ruleSet != null) {
            Rule r = ruleSet.findById(ruleId);
            if (r != null && r.categoryL1() != null) {
                return r.categoryL1();
            }
        }
        if (ruleId != null && ruleId.startsWith("SRS-")) {
            String[] parts = ruleId.split("-");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return "UNKNOWN";
    }

    private static String normalizedMsg(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static String blankToDefault(String s, String d) {
        return (s == null || s.isBlank()) ? d : s;
    }

    private static String sha8(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out).substring(0, 8).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString((s == null ? "" : s).hashCode()).toUpperCase();
        }
    }
}
