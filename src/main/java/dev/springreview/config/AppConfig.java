package dev.springreview.config;

import java.nio.file.Path;
import java.util.List;

/**
 * 合并后的有效配置；密钥不进入此结构。
 */
public record AppConfig(
        Review review,
        Rules rules,
        Schema schema,
        Output output,
        Llm llm,
        StaticTools staticTools,
        Exit exit,
        Logging logging,
        boolean offline,
        boolean validate,
        Path configPath
) {

    public record Review(
            Mode mode,
            Path repoRoot,
            String baseRef,
            String headRef,
            List<String> modulePaths,
            List<String> filePaths
    ) {
    }

    public record Rules(Path dir, Path index) {
    }

    public record Schema(Path reviewReport, Path ruleMetadata) {
    }

    public record Output(Path dir, Path latestDir, String historyPattern, List<OutputFormat> formats) {
    }

    public record Llm(
            boolean enabled,
            String provider,
            String baseUrl,
            String model,
            long timeoutMs,
            int maxConcurrency,
            double temperature,
            int maxTokens
    ) {
    }

    public record StaticTools(boolean checkstyle, boolean pmd, boolean spotbugs) {
    }

    public record Exit(FailOn failOn, boolean strict) {
    }

    public record Logging(String level) {
    }
}