package dev.springreview.scope;

import java.util.List;

/**
 * DIFF 模式下单个变更文件描述。
 */
public record ChangedFile(
        String path,
        String oldPath,
        ChangeType changeType,
        String language,
        boolean binary,
        boolean deleted,
        List<LineRange> lineRanges,
        boolean included
) {

    public enum ChangeType {ADD, MODIFY, DELETE, RENAME, COPY}
}