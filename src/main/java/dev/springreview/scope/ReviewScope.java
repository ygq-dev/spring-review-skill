package dev.springreview.scope;

import java.nio.file.Path;
import java.util.List;

/**
 * M04 resolve 的输出。
 */
public record ReviewScope(
        Mode mode,
        Path repoRoot,
        String baseRef,
        String headRef,
        List<String> modulePaths,
        List<String> filePaths,
        List<String> include,
        List<String> exclude
) {

    public enum Mode {DIFF, MODULE, FILES}
}