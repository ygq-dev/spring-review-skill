package dev.springreview.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import dev.springreview.config.AppConfig;
import dev.springreview.config.CliParams;
import dev.springreview.engine.AggregatedResult;
import dev.springreview.engine.IssueAggregator;
import dev.springreview.engine.RuleEngine;
import dev.springreview.exit.SpringReviewException;
import dev.springreview.llm.LlmClient;
import dev.springreview.llm.LlmClientFactory;
import dev.springreview.observability.Logs;
import dev.springreview.observability.Trace;
import dev.springreview.parser.JavaSourceParser;
import dev.springreview.parser.ParsedBundle;
import dev.springreview.report.MarkdownRenderer;
import dev.springreview.report.ReportBuilder;
import dev.springreview.report.ReportIdGenerator;
import dev.springreview.report.ReportValidator;
import dev.springreview.report.ReportWriter;
import dev.springreview.rules.RuleLoader;
import dev.springreview.rules.RuleSelector;
import dev.springreview.rules.RuleSet;
import dev.springreview.rules.SelectedRules;
import dev.springreview.scope.DiffResult;
import dev.springreview.scope.DiffSourceReader;
import dev.springreview.scope.FileCollector;
import dev.springreview.scope.GitDiffCollector;
import dev.springreview.scope.PathFilters;
import dev.springreview.scope.ReviewScope;
import dev.springreview.scope.ScopeResolver;
import dev.springreview.scope.SourceBundle;
import dev.springreview.tools.RuleMapping;
import dev.springreview.tools.StaticAnalysisFacade;
import dev.springreview.tools.StaticAnalysisResult;
import dev.springreview.tools.ToolRun;
import dev.springreview.tools.ToolStatus;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReviewOrchestrator {

    private static final Logger LOG = Logs.logger("orchestrator");

    public ReviewResult review(AppConfig config, CliParams cli) {
        if (config == null) {
            throw SpringReviewException.cli("AppConfig 为 null");
        }
        Logs.module("M03");
        Instant start = Instant.now();
        String traceId = Trace.current();

        LOG.info("orchestrator start mode={} repo={}",
            config.review().mode(), config.review().repoRoot());

        // 步骤 1
        ReviewScope scope = new ScopeResolver().resolve(config);

        // 步骤 2
        RuleSet ruleSet = new RuleLoader().load(
            config.rules().dir(),
            config.rules().index(),
            config.schema().ruleMetadata(),
            config.review().repoRoot());
        SelectedRules selectedRules = new RuleSelector().select(ruleSet, scope, config);
        if (selectedRules.rules().isEmpty()) {
            LOG.warn("no rules selected for current scope");
        }

        // 步骤 3
        PathFilters filters = new PathFilters(scope.include(), scope.exclude());
        SourceBundle bundle;
        if (scope.mode() == ReviewScope.Mode.DIFF) {
            DiffResult diff = new GitDiffCollector().collect(scope, filters);
            bundle = new DiffSourceReader().read(diff, scope);
        } else {
            bundle = new FileCollector().collect(scope, filters);
        }

        // 步骤 4
        RuleMapping mapping = new RuleMapping(selectedRules);
        StaticAnalysisResult staticResult = new StaticAnalysisFacade().runAll(
            bundle, selectedRules, config, mapping, config.review().repoRoot());

        // 步骤 5
        ParsedBundle parsedBundle = new JavaSourceParser().parseAll(bundle);

        // 步骤 5b：LLM Client（可 offline）
        LlmClient llmClient = LlmClientFactory.create(config, config.review().repoRoot());
        LOG.info("llm client provider={} enabled={}",
            llmClient.provider(), llmClient.enabled());

        RuleEngine.MatchResult engineResult = new RuleEngine(llmClient).match(
            selectedRules, bundle, parsedBundle);

        // 合并候选
        List<dev.springreview.tools.IssueCandidate> allCandidates = new ArrayList<>(
            staticResult.candidates());
        allCandidates.addAll(engineResult.candidates());

        // 步骤 6
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        Map<String, Long> tokenUsage = Map.of(
            "input_tokens", 0L,
            "output_tokens", 0L,
            "total_tokens", 0L);
        AggregatedResult aggregated = new IssueAggregator().aggregate(
            allCandidates, ruleSet, staticResult.toolRuns(), durationMs, tokenUsage);

        // 步骤 7
        Instant now = Instant.now();
        String reportId = new ReportIdGenerator().next(now, config.output().dir());
        JsonNode reportJson = new ReportBuilder().generate(
            reportId, now, scope, bundle, aggregated,
            staticResult.toolRuns(),
            scope.repoRoot().getFileName() == null
                ? "unknown" : scope.repoRoot().getFileName().toString(),
            durationMs, tokenUsage);

        if (config.validate()) {
            new ReportValidator(config.schema().reviewReport()).validateOrThrow(reportJson);
        }

        // 步骤 8
        String markdown = null;
        if (config.output().formats().contains(
            dev.springreview.config.OutputFormat.MARKDOWN)) {
            try {
                markdown = new MarkdownRenderer().render(reportJson);
            } catch (RuntimeException ex) {
                LOG.warn("markdown render failed (non fatal): {}", ex.getMessage());
            }
        }

        ReportWriter.OutputArtifacts artifacts = new ReportWriter()
            .write(reportJson, markdown, config, now);

        LOG.info("scope+collect done mode={} files={} lines={} rules={} issues={}",
            scope.mode(),
            bundle.stats().totalFiles(),
            bundle.stats().totalLines(),
            selectedRules.rules().size(),
            aggregated.issues().size());

        return new ReviewResult(
            artifacts.reportId(),
            traceId,
            artifacts.latestJson(),
            artifacts.latestMarkdown(),
            artifacts.historyDir(),
            aggregated.summary().totalIssues(),
            aggregated.summary().bySeverity(),
            aggregated.summary().blocking(),
            durationMs,
            tokenUsage,
            toolSuccessRate(staticResult.toolRuns()),
            true,
            config.exit().failOn()
        );
    }

    private static double toolSuccessRate(List<ToolRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return 1.0;
        }
        int total = 0;
        int ok = 0;
        for (ToolRun r : runs) {
            if (r.status() == ToolStatus.SKIPPED) {
                continue;
            }
            total++;
            if (r.status() == ToolStatus.SUCCESS) {
                ok++;
            }
        }
        return total == 0 ? 1.0 : (double) ok / total;
    }

    @SuppressWarnings("unused")
    private static List<Step> steps() {
        return List.of(Step.values());
    }
}
