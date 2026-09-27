package dev.springreview.config;

import java.nio.file.Path;
import java.util.List;

/**
 * CLI 显式参数容器；null 表示未提供，用于优先级合并。
 */
public record CliParams(
        Mode mode,
        Path repoRoot,
        String baseRef,
        String headRef,
        List<String> modulePaths,
        List<String> filePaths,
        Path rulesDir,
        Path outputDir,
        Path configPath,
        Boolean offline,
        Boolean noLlm,
        Boolean enableCheckstyle,
        Boolean enablePmd,
        Boolean enableSpotbugs,
        FailOn failOn,
        List<OutputFormat> formats,
        String logLevel,
        Boolean strict,
        Boolean noValidate,
        Boolean noMarkdown,
        String llmProvider,
        String llmModel,
        String llmBaseUrl,
        Long llmTimeout,
        Integer maxConcurrency
) {
}