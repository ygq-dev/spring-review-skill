package dev.springreview.scope;

import java.util.List;

/**
 * M04 collect 的输出；DIFF/MODULE/FILES 共用。
 */
public record SourceBundle(
        String repoRoot,
        ReviewScope.Mode mode,
        List<SourceUnit> units,
        DiffResult diff,
        Stats stats
) {

    public record Stats(
            int totalFiles, int totalLines, int binarySkipped,
            int largeSkipped, int nonJavaSkipped
    ) {
    }
}