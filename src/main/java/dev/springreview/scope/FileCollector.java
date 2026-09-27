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
import java.util.stream.Stream;

/**
 * M06：MODULE / FILES 模式的只读文件收集。
 */
public final class FileCollector {

    private static final Logger LOG = Logs.logger("file-collector");
    private static final long MAX_FILE_BYTES = 1024L * 1024L;

    public SourceBundle collect(ReviewScope scope, PathFilters filters) {
        Logs.module("M06");
        List<SourceUnit> units = new ArrayList<>();
        int totalFiles = 0, totalLines = 0, binary = 0, large = 0, nonJava = 0;

        List<Path> roots = new ArrayList<>();
        if (scope.mode() == ReviewScope.Mode.MODULE) {
            for (String m : scope.modulePaths()) {
                roots.add(scope.repoRoot().resolve(m));
            }
        } else {
            for (String f : scope.filePaths()) {
                roots.add(scope.repoRoot().resolve(f));
            }
        }

        for (Path root : roots) {
            if (!Files.exists(root)) {
                throw SpringReviewException.scope("路径不存在: " + root, null);
            }
            if (Files.isRegularFile(root)) {
                SourceUnit u = readUnit(scope.repoRoot(), root, filters);
                if (u != null) {
                    units.add(u);
                    totalFiles++;
                    totalLines += u.lines();
                }
            } else {
                try (Stream<Path> stream = Files.walk(root)) {
                    List<Path> javaFiles = stream
                            .filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java"))
                            .toList();
                    for (Path p : javaFiles) {
                        Path rel = scope.repoRoot().relativize(p);
                        if (!filters.includes(rel)) {
                            continue;
                        }
                        SourceUnit u = readUnit(scope.repoRoot(), p, filters);
                        if (u != null) {
                            units.add(u);
                            totalFiles++;
                            totalLines += u.lines();
                        }
                    }
                } catch (IOException ex) {
                    throw SpringReviewException.scope("扫描失败: " + root, ex);
                }
            }
        }
        LOG.info("files collected mode={} files={} lines={}", scope.mode(), totalFiles, totalLines);
        return new SourceBundle(
                scope.repoRoot().toString(),
                scope.mode(),
                List.copyOf(units),
                null,
                new SourceBundle.Stats(totalFiles, totalLines, binary, large, nonJava)
        );
    }

    private SourceUnit readUnit(Path repoRoot, Path file, PathFilters filters) {
        try {
            long size = Files.size(file);
            if (size > MAX_FILE_BYTES) {
                LOG.warn("file too large, skip: {} ({} bytes)", file, size);
                return null;
            }
            byte[] bytes = Files.readAllBytes(file);
            String content = new String(bytes, StandardCharsets.UTF_8);
            Path rel = repoRoot.relativize(file);
            int lines = (int) content.lines().count();
            String hash = sha256(content);
            return new SourceUnit(
                    rel.toString().replace('\\', '/'),
                    "java",
                    content,
                    hash,
                    lines,
                    "_root",
                    List.of()
            );
        } catch (IOException ex) {
            throw SpringReviewException.scope("读取失败: " + file, ex);
        }
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