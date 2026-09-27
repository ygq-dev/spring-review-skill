package dev.springreview.tools;

import java.util.List;

public record StaticAnalysisResult(
    List<ToolRun> toolRuns,
    List<IssueCandidate> candidates
) {
}
