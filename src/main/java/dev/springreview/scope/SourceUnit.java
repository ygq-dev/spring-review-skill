package dev.springreview.scope;

import java.util.List;

/**
 * 只读源码快照。
 */
public record SourceUnit(
        String path,
        String language,
        String content,
        String hash,
        int lines,
        String module,
        List<LineRange> diffChunks
) {
}