package dev.springreview.report;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * M15：把 A6 报告渲染为 Markdown。只读报告，不重算。
 */
public final class MarkdownRenderer {

    private static final List<String> SEV_ORDER =
        List.of("BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO");

    public String render(JsonNode report) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# Spring Review Report\n\n");

        // 元信息
        sb.append("## 元信息\n\n");
        kv(sb, "report_id", report.path("report_id").asText(""));
        kv(sb, "schema_version", report.path("schema_version").asText(""));
        kv(sb, "generated_at", report.path("generated_at").asText(""));
        sb.append('\n');

        // 范围
        JsonNode scope = report.path("scope");
        sb.append("## 范围\n\n");
        kv(sb, "mode", scope.path("mode").asText(""));
        kv(sb, "project", scope.path("project").asText(""));
        kv(sb, "base_ref", scope.path("base_ref").asText(""));
        kv(sb, "head_ref", scope.path("head_ref").asText(""));
        kv(sb, "diff_lines", scope.path("diff_lines").asText("0"));
        JsonNode paths = scope.path("paths");
        if (paths.isArray() && !paths.isEmpty()) {
            sb.append("- paths:\n");
            for (JsonNode p : paths) {
                sb.append("  - ").append(p.asText()).append('\n');
            }
        }
        sb.append('\n');

        // 摘要
        JsonNode summary = report.path("summary");
        sb.append("## 摘要\n\n");
        kv(sb, "total_issues", summary.path("total_issues").asText("0"));
        kv(sb, "blocking", summary.path("blocking").asText("0"));
        kv(sb, "duration_ms", summary.path("duration_ms").asText("0"));
        JsonNode bySev = summary.path("by_severity");
        if (bySev.isObject()) {
            sb.append("- by_severity:\n");
            for (String k : SEV_ORDER) {
                sb.append("  - ").append(k).append(": ")
                    .append(bySev.path(k).asInt(0)).append('\n');
            }
        }
        sb.append('\n');

        // 问题，按 severity 分组
        JsonNode issues = report.path("issues");
        sb.append("## 问题\n\n");
        if (!issues.isArray() || issues.isEmpty()) {
            sb.append("_未发现问题。_\n\n");
        } else {
            for (String sev : SEV_ORDER) {
                StringBuilder section = new StringBuilder();
                int count = 0;
                for (JsonNode i : issues) {
                    if (!sev.equals(i.path("severity").asText())) {
                        continue;
                    }
                    count++;
                    section.append("### [").append(sev).append("] ")
                        .append(i.path("issue_id").asText("")).append("\n\n");
                    kv(section, "rule_id", i.path("rule_id").asText(""));
                    kv(section, "confidence", i.path("confidence").asText(""));
                    kv(section, "detection_method", i.path("detection_method").asText(""));
                    section.append("- location: `")
                        .append(i.path("file").asText(""))
                        .append(':').append(i.path("line").asInt(0))
                        .append(':').append(i.path("column").asInt(0))
                        .append("`\n");
                    if (i.hasNonNull("message")) {
                        section.append("- message: ").append(i.path("message").asText()).append('\n');
                    }
                    if (i.hasNonNull("evidence")) {
                        section.append("- evidence: `")
                            .append(i.path("evidence").asText().replace("`", "'"))
                            .append("`\n");
                    }
                    if (i.hasNonNull("remediation")) {
                        section.append("- remediation: ").append(i.path("remediation").asText()).append('\n');
                    }
                    if (i.hasNonNull("fingerprint")) {
                        section.append("- fingerprint: ").append(i.path("fingerprint").asText()).append('\n');
                    }
                    section.append('\n');
                }
                if (count > 0) {
                    sb.append("### ").append(sev).append('\n');
                    sb.append('\n');
                    sb.append(section);
                }
            }
        }

        // 工具运行
        JsonNode toolRuns = report.path("tool_runs");
        sb.append("## 工具运行\n\n");
        if (!toolRuns.isArray() || toolRuns.isEmpty()) {
            sb.append("_无工具运行记录。_\n\n");
        } else {
            for (JsonNode t : toolRuns) {
                sb.append("- ").append(t.path("tool").asText(""))
                    .append(" v").append(t.path("tool_version").asText(""))
                    .append(" status=").append(t.path("status").asText(""))
                    .append(" duration_ms=").append(t.path("duration_ms").asLong(0))
                    .append(" exit_code=").append(t.path("exit_code").asInt(0));
                if (t.hasNonNull("error_message")) {
                    sb.append(" error=").append(t.path("error_message").asText());
                }
                sb.append('\n');
            }
            sb.append('\n');
        }

        // 指标
        JsonNode metrics = report.path("metrics");
        sb.append("## 指标\n\n");
        kv(sb, "total_files_scanned", metrics.path("total_files_scanned").asText("0"));
        kv(sb, "total_lines_scanned", metrics.path("total_lines_scanned").asText("0"));
        kv(sb, "duration_ms", metrics.path("duration_ms").asText("0"));
        kv(sb, "tool_success_rate", metrics.path("tool_success_rate").asText("1.0"));
        sb.append('\n');

        return sb.toString();
    }

    private static void kv(StringBuilder sb, String k, String v) {
        sb.append("- ").append(k).append(": `")
            .append(v == null ? "" : v).append("`\n");
    }
}
