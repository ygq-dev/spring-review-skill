package dev.springreview.scope;

import dev.springreview.exit.SpringReviewException;
import dev.springreview.observability.Logs;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * DIFF 模式下读取新侧源码为 SourceUnit。
 * 只读；不写回目标仓库；diffChunks 来自 DiffResult 的 lineRanges。
 */
public final class DiffSourceReader {

    private static final Logger LOG = Logs.logger("diff-source");
    private static final long MAX_FILE_BYTES = 1024L * 1024L;

    public SourceBundle read(DiffResult diff, ReviewScope scope) {
        Logs.module("M04");
        List<SourceUnit> units = new ArrayList<>();
        int totalLines = 0;

        for (ChangedFile cf : diff.changedFiles()) {
            if (!cf.included() || cf.deleted()) {
                continue;
            }
            Path file = scope.repoRoot().resolve(cf.path());
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                LOG.warn("changed file not found, skip: {}", cf.path());
                continue;
            }
            try {
                long size = Files.size(file);
                if (size > MAX_FILE_BYTES) {
                    LOG.warn("changed file too large, skip: {} ({} bytes)", cf.path(), size);
                    continue;
                }
                byte[] bytes = Files.readAllBytes(file);
                String content = new String(bytes, StandardCharsets.UTF_8);
                int lines = (int) content.lines().count();
                String hash = sha256(content);
                units.add(new SourceUnit(
                    cf.path(),
                    cf.language(),
                    content,
                    hash,
                    lines,
                    "_root",
                    cf.lineRanges()
                ));
                totalLines += lines;
            } catch (IOException ex) {
                throw SpringReviewException.scope("读取变更文件失败: " + cf.path(), ex);
            }
        }

        int binary = diff.stats().binarySkipped();
        int large = diff.stats().largeSkipped();
        int nonJava = diff.stats().nonJavaSkipped();
        LOG.info("diff source read units={} lines={}", units.size(), totalLines);
        return new SourceBundle(
            scope.repoRoot().toString(),
            ReviewScope.Mode.DIFF,
            List.copyOf(units),
            diff,
            new SourceBundle.Stats(units.size(), totalLines, binary, large, nonJava)
        );
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
