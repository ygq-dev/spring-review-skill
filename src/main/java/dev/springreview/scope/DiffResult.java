package dev.springreview.scope;

import java.util.List;
import java.util.Map;

/**
 * M05 输出：变更文件、行范围、统计。
 */
public record DiffResult(
        String repoRoot,
        String baseRef,
        String headRef,
        String resolvedBaseCommit,
        String resolvedHeadCommit,
        List<ChangedFile> changedFiles,
        Map<String, List<LineRange>> lineRanges,
        Stats stats,
        List<String> warnings
) {

    public record Stats(
            int total, int added, int modified, int deleted, int renamed,
            int binarySkipped, int largeSkipped, int nonJavaSkipped, int collected
    ) {
    }
}