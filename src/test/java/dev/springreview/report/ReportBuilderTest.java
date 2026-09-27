package dev.springreview.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springreview.engine.AggregatedResult;
import dev.springreview.engine.Issue;
import dev.springreview.engine.Summary;
import dev.springreview.scope.ReviewScope;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import dev.springreview.tools.ToolRun;
import dev.springreview.tools.ToolStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportBuilderTest {

    @Test
    void generatesMinimalReport() {
        ReviewScope scope = new ReviewScope(
            ReviewScope.Mode.FILES, Path.of("/tmp/repo"),
            null, null, List.of(), List.of("src/A.java"),
            List.of("**/*.java"), List.of());
        SourceBundle bundle = new SourceBundle("/tmp/repo", ReviewScope.Mode.FILES,
            List.of(new SourceUnit("src/A.java", "java", "class A {}",
                "sha256:x", 1, "_root", List.of())),
            null,
            new SourceBundle.Stats(1, 1, 0, 0, 0));

        Issue issue = new Issue("ISSUE-1A2B3C4D", "SRS-SEC-01-001", "BLOCKER", "HIGH",
            "REGEX", "src/A.java", 1, 1, "password=x", "硬编码密钥",
            "使用环境变量", "CUSTOM", null, null, null,
            "fp1", "OPEN", List.of());
        Summary summary = new Summary(1,
            Map.of("BLOCKER", 1, "CRITICAL", 0, "MAJOR", 0, "MINOR", 0, "INFO", 0),
            Map.of("SEC", 1), 1, 100, Map.of("total_tokens", 0L));
        AggregatedResult aggregated = new AggregatedResult(List.of(issue), summary, List.of());

        JsonNode report = new ReportBuilder().generate(
            "RPT-2026-0001", Instant.parse("2026-09-25T00:00:00Z"),
            scope, bundle, aggregated,
            List.of(new ToolRun(dev.springreview.tools.Tool.CHECKSTYLE, "10.17.0",
                ToolStatus.SUCCESS, Instant.now(), Instant.now(), 100, 0, 0,
                null, null)),
            "test-project", 500, Map.of("total_tokens", 0L));

        assertThat(report.path("report_id").asText()).isEqualTo("RPT-2026-0001");
        assertThat(report.path("schema_version").asText()).isEqualTo("1.0.0");
        assertThat(report.path("scope").path("mode").asText()).isEqualTo("FILES");
        assertThat(report.path("summary").path("total_issues").asInt()).isEqualTo(1);
        assertThat(report.path("issues")).hasSize(1);
        assertThat(report.path("issues").get(0).path("issue_id").asText())
            .isEqualTo("ISSUE-1A2B3C4D");
        assertThat(report.path("tool_runs")).hasSize(1);
        assertThat(report.path("metrics").path("total_files_scanned").asInt()).isEqualTo(1);
    }

    @Test
    void validatesAgainstA6Schema() throws Exception {
        java.nio.file.Path schemaPath = java.nio.file.Path.of("schema/review-report.schema.json");
        org.junit.jupiter.api.Assumptions.assumeTrue(
            java.nio.file.Files.exists(schemaPath),
            "schema/review-report.schema.json 不存在，跳过");

        ReviewScope scope = new ReviewScope(
            ReviewScope.Mode.FILES, Path.of("/tmp/repo"),
            null, null, List.of(), List.of("src/A.java"),
            List.of("**/*.java"), List.of());
        SourceBundle bundle = new SourceBundle("/tmp/repo", ReviewScope.Mode.FILES,
            List.of(new SourceUnit("src/A.java", "java", "class A {}",
                "sha256:x", 1, "_root", List.of())),
            null,
            new SourceBundle.Stats(1, 1, 0, 0, 0));
        Summary summary = new Summary(0,
            Map.of("BLOCKER", 0, "CRITICAL", 0, "MAJOR", 0, "MINOR", 0, "INFO", 0),
            Map.of(), 0, 100, Map.of("total_tokens", 0L));
        AggregatedResult aggregated = new AggregatedResult(List.of(), summary, List.of());

        JsonNode report = new ReportBuilder().generate(
            "RPT-2026-0001", Instant.parse("2026-09-25T00:00:00Z"),
            scope, bundle, aggregated, List.of(), "test", 100,
            Map.of("total_tokens", 0L));

        new ReportValidator(schemaPath).validateOrThrow(report);

        // 序列化稳定性
        String json = new ObjectMapper().writeValueAsString(report);
        assertThat(json).contains("RPT-2026-0001");
    }
}
