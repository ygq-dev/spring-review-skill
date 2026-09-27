package dev.springreview.engine;

import dev.springreview.tools.ToolRun;

import java.util.List;

public record AggregatedResult(
    List<Issue> issues,
    Summary summary,
    List<ToolRun> toolRuns
) {
}
