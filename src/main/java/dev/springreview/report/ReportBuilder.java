package dev.springreview.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.springreview.engine.AggregatedResult;
import dev.springreview.engine.Issue;
import dev.springreview.engine.Summary;
import dev.springreview.scope.DiffResult;
import dev.springreview.scope.ReviewScope;
import dev.springreview.scope.SourceBundle;
import dev.springreview.tools.ToolRun;

import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * M14：组装 A6 兼容的 ReviewReport（以 ObjectNode 表达）。
 * 字段顺序稳定；additionalProperties 由 schema 控制。
 */
public final class ReportBuilder {

    private static final String SCHEMA_VERSION = "1.0.0";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ObjectNode generate(String reportId,
                               Instant generatedAt,
                               ReviewScope scope,
                               SourceBundle bundle,
                               AggregatedResult aggregated,
                               List<ToolRun> toolRuns,
                               String projectName,
                               long durationMs,
                               Map<String, Long> tokenUsage) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("report_id", reportId);
        root.put("schema_version", SCHEMA_VERSION);
        root.put("generated_at", DateTimeFormatter.ISO_INSTANT.format(generatedAt));

        root.set("scope", buildScope(scope, bundle, projectName));
        root.set("summary", buildSummary(aggregated.summary(), durationMs, tokenUsage));

        ArrayNode issues = root.putArray("issues");
        for (Issue i : aggregated.issues()) {
            issues.add(buildIssue(i));
        }

        ArrayNode toolRunsNode = root.putArray("tool_runs");
        for (ToolRun t : toolRuns) {
            toolRunsNode.add(buildToolRun(t));
        }

        root.set("metrics", buildMetrics(bundle, aggregated, toolRuns, durationMs));
        return root;
    }

    private ObjectNode buildScope(ReviewScope scope, SourceBundle bundle, String projectName) {
        ObjectNode s = MAPPER.createObjectNode();
        s.put("mode", scope.mode().name());
        s.put("project", projectName == null ? "unknown" : projectName);
        s.put("base_ref", scope.baseRef() == null ? "" : scope.baseRef());
        s.put("head_ref", scope.headRef() == null ? "" : scope.headRef());
        ArrayNode paths = s.putArray("paths");
        if (bundle.units().isEmpty()) {
            paths.add(scope.repoRoot().toString().replace('\\', '/'));
        } else {
            for (var u : bundle.units()) {
                paths.add(u.path());
            }
        }
        s.put("diff_lines", diffLines(bundle));
        return s;
    }

    private int diffLines(SourceBundle bundle) {
        DiffResult d = bundle.diff();
        if (d == null || d.changedFiles() == null) {
            return 0;
        }
        int total = 0;
        for (var cf : d.changedFiles()) {
            if (cf.lineRanges() == null) {
                continue;
            }
            for (var r : cf.lineRanges()) {
                total += r.length();
            }
        }
        return total;
    }

    private ObjectNode buildSummary(Summary s, long durationMs, Map<String, Long> tokenUsage) {
        ObjectNode out = MAPPER.createObjectNode();
        out.put("total_issues", s.totalIssues());

        ObjectNode bySeverity = out.putObject("by_severity");
        for (String k : List.of("BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO")) {
            bySeverity.put(k, s.bySeverity().getOrDefault(k, 0));
        }

        ObjectNode byCategory = out.putObject("by_category");
        s.byCategory().forEach((k, v) -> {
            if (!"UNKNOWN".equals(k) && v > 0) {
                byCategory.put(k, v);
            }
        });

        out.put("blocking", s.blocking());
        out.put("duration_ms", durationMs);

        ObjectNode tu = out.putObject("token_usage");
        long total = tokenUsage == null ? 0 : tokenUsage.getOrDefault("total_tokens", 0L);
        long in = tokenUsage == null ? 0 : tokenUsage.getOrDefault("input_tokens", 0L);
        long outT = tokenUsage == null ? 0 : tokenUsage.getOrDefault("output_tokens", 0L);
        tu.put("input_tokens", in);
        tu.put("output_tokens", outT);
        tu.put("total_tokens", total);
        return out;
    }

    private ObjectNode buildIssue(Issue i) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("issue_id", i.issueId());
        n.put("rule_id", i.ruleId());
        n.put("severity", i.severity());
        n.put("confidence", i.confidence());
        n.put("detection_method", i.detectionMethod());
        n.put("file", i.file());
        n.put("line", Math.max(1, i.line()));
        n.put("column", Math.max(1, i.column()));
        n.put("evidence", orEmpty(i.evidence()));
        n.put("message", orEmpty(i.message()));
        n.put("remediation", orEmpty(i.remediation()));
        if (i.tool() != null && !i.tool().isBlank()) {
            n.put("tool", i.tool());
        }
        if (i.toolRuleId() != null && !i.toolRuleId().isBlank()) {
            n.put("tool_rule_id", i.toolRuleId());
        }
        if (i.testCaseId() != null && !i.testCaseId().isBlank()) {
            n.put("test_case_id", i.testCaseId());
        }
        if (i.snippet() != null && !i.snippet().isBlank()) {
            n.put("snippet", i.snippet());
        }
        if (i.fingerprint() != null && !i.fingerprint().isBlank()) {
            n.put("fingerprint", i.fingerprint());
        }
        if (i.issueStatus() != null && !i.issueStatus().isBlank()) {
            n.put("issue_status", i.issueStatus());
        }
        if (i.tags() != null && !i.tags().isEmpty()) {
            ArrayNode tags = n.putArray("tags");
            for (String t : i.tags()) {
                tags.add(t);
            }
        }
        return n;
    }

    private ObjectNode buildToolRun(ToolRun t) {
        ObjectNode n = MAPPER.createObjectNode();
        n.put("tool", t.tool().name());
        n.put("tool_version", orEmpty(t.toolVersion()));
        n.put("status", t.status().name());
        n.put("started_at", DateTimeFormatter.ISO_INSTANT.format(t.startedAt()));
        n.put("ended_at", DateTimeFormatter.ISO_INSTANT.format(t.endedAt()));
        n.put("duration_ms", t.durationMs());
        n.put("exit_code", t.exitCode());
        if (t.issuesCount() > 0) {
            n.put("issues_count", t.issuesCount());
        }
        if (t.errorMessage() != null && !t.errorMessage().isBlank()) {
            n.put("error_message", t.errorMessage());
        }
        return n;
    }

    private ObjectNode buildMetrics(SourceBundle bundle,
                                    AggregatedResult aggregated,
                                    List<ToolRun> toolRuns,
                                    long durationMs) {
        ObjectNode m = MAPPER.createObjectNode();
        m.put("total_files_scanned", bundle.stats().totalFiles());
        m.put("total_lines_scanned", bundle.stats().totalLines());
        m.put("duration_ms", durationMs);
        m.put("tool_success_rate", toolSuccessRate(toolRuns));
        return m;
    }

    private double toolSuccessRate(List<ToolRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return 1.0;
        }
        int total = 0;
        int success = 0;
        for (ToolRun r : runs) {
            if (r.status() == dev.springreview.tools.ToolStatus.SKIPPED) {
                continue;
            }
            total++;
            if (r.status() == dev.springreview.tools.ToolStatus.SUCCESS) {
                success++;
            }
        }
        return total == 0 ? 1.0 : (double) success / total;
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
