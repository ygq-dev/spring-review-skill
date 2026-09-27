package dev.springreview.exit;

import dev.springreview.config.AppConfig;
import dev.springreview.config.FailOn;
import dev.springreview.config.Mode;
import dev.springreview.config.OutputFormat;
import dev.springreview.orchestration.ReviewResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExitCodeMapperTest {

    private static AppConfig configWithFailOn(FailOn failOn) {
        return new AppConfig(
                new AppConfig.Review(Mode.DIFF, Path.of("."), "HEAD~1", "HEAD", List.of(), List.of()),
                new AppConfig.Rules(Path.of("rules"), Path.of("rules/index.json")),
                new AppConfig.Schema(Path.of("s.json"), Path.of("r.json")),
                new AppConfig.Output(Path.of("reports"), Path.of("reports/latest"), "h", List.of(OutputFormat.JSON)),
                new AppConfig.Llm(false, "deepseek", "", "", 60000, 4, 0.1, 4096),
                new AppConfig.StaticTools(true, true, false),
                new AppConfig.Exit(failOn, false),
                new AppConfig.Logging("INFO"),
                false, true, null
        );
    }

    private static ReviewResult result(Map<String, Integer> bySeverity, boolean schemaValid) {
        int total = bySeverity.values().stream().mapToInt(Integer::intValue).sum();
        return new ReviewResult(
                "RPT-2026-0001", "trace", Path.of("a.json"), Path.of("a.md"),
                Path.of("h"), total, bySeverity, 0, 100, Map.of(), 1.0,
                schemaValid, FailOn.CRITICAL);
    }

    @Test
    void successWhenNoIssues() {
        ReviewResult r = result(Map.of("BLOCKER", 0, "CRITICAL", 0), true);
        assertThat(ExitCodeMapper.map(r, configWithFailOn(FailOn.CRITICAL))).isZero();
    }

    @Test
    void failOnCriticalHit() {
        ReviewResult r = result(Map.of("CRITICAL", 1, "BLOCKER", 0), true);
        assertThat(ExitCodeMapper.map(r, configWithFailOn(FailOn.CRITICAL))).isEqualTo(ExitCodes.FAIL_ON_HIT);
    }

    @Test
    void failOnNever() {
        ReviewResult r = result(Map.of("BLOCKER", 1, "CRITICAL", 1), true);
        assertThat(ExitCodeMapper.map(r, configWithFailOn(FailOn.NEVER))).isZero();
    }

    @Test
    void schemaInvalid_mapsToReport() {
        ReviewResult r = result(Map.of("INFO", 1), false);
        assertThat(ExitCodeMapper.map(r, configWithFailOn(FailOn.INFO))).isEqualTo(ExitCodes.REPORT);
    }

    @Test
    void nullResult_mapsInternal() {
        assertThat(ExitCodeMapper.map(null, configWithFailOn(FailOn.CRITICAL))).isEqualTo(ExitCodes.INTERNAL);
    }

    @Test
    void exceptionMapping() {
        assertThat(ExitCodeMapper.mapException(SpringReviewException.cli("x"))).isEqualTo(ExitCodes.CLI_CONFIG);
        assertThat(ExitCodeMapper.mapException(SpringReviewException.scope("x", null))).isEqualTo(ExitCodes.SCOPE);
        assertThat(ExitCodeMapper.mapException(SpringReviewException.ruleLoad("x", null))).isEqualTo(ExitCodes.RULE_LOAD);
        assertThat(ExitCodeMapper.mapException(SpringReviewException.report("x", null))).isEqualTo(ExitCodes.REPORT);
        assertThat(ExitCodeMapper.mapException(new RuntimeException("x"))).isEqualTo(ExitCodes.INTERNAL);
    }
}