package dev.springreview.exit;

import dev.springreview.orchestration.ReviewResult;

import java.io.PrintStream;

/**
 * stdout 摘要输出。失败时降级 stderr 简化摘要。
 */
public final class SummaryPrinter {

    private SummaryPrinter() {
    }

    public static void print(PrintStream out, ReviewResult result, int exitCode) {
        if (result == null) {
            safeErr("summary: <no result> exit_code=" + exitCode);
            return;
        }
        StringBuilder sb = new StringBuilder(256);
        line(sb, "report_id", result.reportId());
        line(sb, "trace_id", result.traceId());
        line(sb, "total_issues", String.valueOf(result.totalIssues()));
        line(sb, "by_severity", String.valueOf(result.bySeverity()));
        line(sb, "blocking", String.valueOf(result.blocking()));
        line(sb, "duration_ms", String.valueOf(result.durationMs()));
        line(sb, "tool_success_rate", String.valueOf(result.toolSuccessRate()));
        line(sb, "latest_json", String.valueOf(result.latestJson()));
        line(sb, "latest_md", String.valueOf(result.latestMarkdown()));
        line(sb, "history", String.valueOf(result.historyDir()));
        line(sb, "exit_code", String.valueOf(exitCode));
        try {
            out.print(sb);
            out.flush();
        } catch (Throwable t) {
            safeErr(sb.toString());
        }
    }

    private static void line(StringBuilder sb, String k, String v) {
        sb.append(k).append('=').append(v == null ? "" : v).append('\n');
    }

    private static void safeErr(String msg) {
        try {
            System.err.println(msg);
        } catch (Throwable ignored) {
            // 无可用输出通道时忽略
        }
    }
}