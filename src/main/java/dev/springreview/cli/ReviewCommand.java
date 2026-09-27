package dev.springreview.cli;

import dev.springreview.config.AppConfig;
import dev.springreview.config.CliParams;
import dev.springreview.config.ConfigLoader;
import dev.springreview.config.FailOn;
import dev.springreview.config.Mode;
import dev.springreview.config.OutputFormat;
import dev.springreview.exit.ExitCodeMapper;
import dev.springreview.exit.ExitCodes;
import dev.springreview.exit.SpringReviewException;
import dev.springreview.exit.SummaryPrinter;
import dev.springreview.observability.Logs;
import dev.springreview.observability.Trace;
import dev.springreview.orchestration.ReviewOrchestrator;
import dev.springreview.orchestration.ReviewResult;
import org.slf4j.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI 入口，参数严格对齐 E3 CLI 参数总览。
 */
@Command(
        name = "spring-review",
        mixinStandardHelpOptions = true,
        version = "spring-review 0.1.0-SNAPSHOT (Java 17+, Spring Boot 3.x)",
        description = "只读 Spring 代码审查；输出 Markdown + JSON；不修改目标仓库。"
)
public final class ReviewCommand implements Callable<Integer> {

    private static final Logger LOG = Logs.logger("cli");

    @Option(names = "--mode", description = "审查模式：DIFF / MODULE / FILES")
    Mode mode;

    @Option(names = "--repo", description = "目标仓库根目录")
    Path repoRoot;

    @Option(names = "--base", description = "DIFF 基准，如 HEAD~1")
    String baseRef;

    @Option(names = "--head", description = "DIFF 终点，默认 HEAD")
    String headRef;

    @Option(names = "--module", description = "模块路径，可重复", split = ",")
    List<String> modulePaths;

    @Option(names = "--files", description = "文件路径，逗号分隔或可重复", split = ",")
    List<String> filePaths;

    @Option(names = "--rules-dir", description = "规则目录，默认 rules")
    Path rulesDir;

    @Option(names = "--output-dir", description = "输出目录，默认 reports")
    Path outputDir;

    @Option(names = "--config", description = "配置文件路径")
    Path configPath;

    @Option(names = "--offline", description = "禁止 LLM 调用")
    Boolean offline;

    @Option(names = "--no-llm", description = "禁用 LLM")
    Boolean noLlm;

    @Option(names = "--enable-checkstyle", description = "启用 Checkstyle")
    Boolean enableCheckstyle;

    @Option(names = "--enable-pmd", description = "启用 PMD")
    Boolean enablePmd;

    @Option(names = "--enable-spotbugs", description = "启用 SpotBugs")
    Boolean enableSpotbugs;

    @Option(names = "--fail-on", description = "退出码阈值：BLOCKER/CRITICAL/MAJOR/MINOR/INFO/NEVER")
    FailOn failOn;

    @Option(names = "--format", description = "输出格式：json,markdown", split = ",")
    List<OutputFormat> formats;

    @Option(names = "--log-level", description = "日志级别，默认 INFO")
    String logLevel;

    @Option(names = "--strict", description = "严格模式，降级计入 failOn")
    Boolean strict;

    @Option(names = "--no-markdown", description = "不输出 Markdown")
    Boolean noMarkdown;

    @Option(names = "--no-validate", description = "跳过报告 Schema 校验")
    Boolean noValidate;

    @Option(names = "--llm-provider", description = "LLM 提供方")
    String llmProvider;

    @Option(names = "--llm-model", description = "LLM 模型")
    String llmModel;

    @Option(names = "--llm-base-url", description = "LLM baseUrl")
    String llmBaseUrl;

    @Option(names = "--llm-timeout", description = "LLM 超时毫秒")
    Long llmTimeout;

    @Option(names = "--max-concurrency", description = "最大并发")
    Integer maxConcurrency;

    @Override
    public Integer call() {
        String traceId = Trace.newTraceId();
        Trace.bind(traceId);
        Logs.module("M01");
        int exitCode = ExitCodes.INTERNAL;
        try {
            CliParams cli = toCliParams();
            AppConfig config = new ConfigLoader().load(cli, System.getenv(), Path.of("."));
            ReviewResult result = new ReviewOrchestrator().review(config, cli);
            exitCode = ExitCodeMapper.map(result, config);
            SummaryPrinter.print(System.out, result, exitCode);
            return exitCode;
        } catch (SpringReviewException ex) {
            LOG.error("审查失败 code={} msg={}", ex.exitCode(), ex.getMessage());
            exitCode = ex.exitCode();
            return exitCode;
        } catch (Throwable t) {
            LOG.error("未预期异常", t);
            exitCode = ExitCodes.INTERNAL;
            return exitCode;
        } finally {
            Trace.clear();
            Logs.clear();
        }
    }

    private CliParams toCliParams() {
        return new CliParams(
                mode, repoRoot, baseRef, headRef,
                modulePaths, filePaths,
                rulesDir, outputDir, configPath,
                offline, noLlm,
                enableCheckstyle, enablePmd, enableSpotbugs,
                failOn, formats,
                logLevel, strict, noValidate, noMarkdown,
                llmProvider, llmModel, llmBaseUrl, llmTimeout, maxConcurrency
        );
    }
}