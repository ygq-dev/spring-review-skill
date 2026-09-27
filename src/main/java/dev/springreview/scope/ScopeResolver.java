package dev.springreview.scope;

import dev.springreview.config.AppConfig;
import dev.springreview.config.Mode;
import dev.springreview.exit.SpringReviewException;
import dev.springreview.observability.Logs;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * M04 范围解析：按模式解析为 ReviewScope。
 */
public final class ScopeResolver {

    private static final Logger LOG = Logs.logger("scope");

    public ReviewScope resolve(AppConfig config) {
        Logs.module("M04");
        AppConfig.Review r = config.review();
        if (r == null || r.mode() == null) {
            throw SpringReviewException.cli("review.mode 未配置");
        }
        Path repoRoot = r.repoRoot();
        if (repoRoot == null || !Files.exists(repoRoot) || !Files.isReadable(repoRoot)) {
            throw SpringReviewException.scope("repoRoot 不可读: " + repoRoot, null);
        }
        ReviewScope.Mode mode = map(r.mode());
        List<String> modules = r.modulePaths() == null ? List.of() : r.modulePaths();
        List<String> files = r.filePaths() == null ? List.of() : r.filePaths();

        switch (mode) {
            case DIFF -> {
                if (r.baseRef() == null || r.baseRef().isBlank()) {
                    throw SpringReviewException.cli("DIFF 模式需要 baseRef");
                }
            }
            case MODULE -> {
                if (modules.isEmpty()) {
                    throw SpringReviewException.cli("MODULE 模式需要 modulePaths");
                }
            }
            case FILES -> {
                if (files.isEmpty()) {
                    throw SpringReviewException.cli("FILES 模式需要 filePaths");
                }
            }
        }
        LOG.info("scope resolved mode={} repo={} base={} head={}",
                mode, repoRoot, r.baseRef(), r.headRef());
        return new ReviewScope(mode, repoRoot, r.baseRef(), r.headRef(), modules, files,
                List.of("**/*.java"), List.of());
    }

    private static ReviewScope.Mode map(Mode m) {
        return switch (m) {
            case DIFF -> ReviewScope.Mode.DIFF;
            case MODULE -> ReviewScope.Mode.MODULE;
            case FILES -> ReviewScope.Mode.FILES;
        };
    }
}