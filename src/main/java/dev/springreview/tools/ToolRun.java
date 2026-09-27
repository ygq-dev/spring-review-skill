package dev.springreview.tools;

import java.nio.file.Path;
import java.time.Instant;

/**
 * 单次工具运行记录，对齐 A6 tool_runs。
 */
public record ToolRun(
    Tool tool,
    String toolVersion,
    ToolStatus status,
    Instant startedAt,
    Instant endedAt,
    long durationMs,
    int exitCode,
    int issuesCount,
    String errorMessage,
    Path rawOutputPath
) {

    public static ToolRun skipped(Tool tool, String version, String reason) {
        Instant now = Instant.now();
        return new ToolRun(tool, version, ToolStatus.SKIPPED, now, now, 0, 0, 0, reason, null);
    }
}
