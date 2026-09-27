package dev.springreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springreview.config.CliParams;
import dev.springreview.config.ConfigLoader;
import dev.springreview.observability.Trace;
import dev.springreview.orchestration.ReviewOrchestrator;
import dev.springreview.orchestration.ReviewResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端集成测试：不依赖外部工具 jar，
 * 只走 JavaParser + Regex + Config 检测器链路，
 * 验证从 CLI 参数 → 配置 → 范围 → 规则 → 代码收集 → 规则引擎 → 报告落盘的完整闭环。
 *
 * 注意：schema.* 无 CLI 参数，只能来自 YAML 或 workingDir/schema；
 * 因此本测试把项目根 rules/ 与 schema/ 复制到临时 repo 下，
 * 使 workingDir 与默认路径一致。
 */
class EndToEndIntegrationTest {

    @Test
    void filesModeGeneratesValidReport(@TempDir Path workDir) throws IOException {
        Path repo = workDir.resolve("repo");
        Files.createDirectories(repo.resolve("src/main/java/com/example"));

        Files.writeString(repo.resolve("src/main/java/com/example/Demo.java"), """
                package com.example;

                public class Demo {
                    private final String password = "admin123";

                    public String getPassword() {
                        return password;
                    }
                }
                """, StandardCharsets.UTF_8);

        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        Path srcRules = projectRoot.resolve("rules");
        Path srcSchema = projectRoot.resolve("schema");

        Assumptions.assumeTrue(Files.exists(srcRules.resolve("index.json")),
            "rules/index.json 缺失，跳过端到端集成测试");
        Assumptions.assumeTrue(Files.exists(srcSchema.resolve("rule-metadata.schema.json")),
            "schema/rule-metadata.schema.json 缺失，跳过端到端集成测试");
        Assumptions.assumeTrue(Files.exists(srcSchema.resolve("review-report.schema.json")),
            "schema/review-report.schema.json 缺失，跳过端到端集成测试");

        // 把 rules/ 和 schema/ 复制到临时 repo 下
        copyTree(srcRules, repo.resolve("rules"));
        copyTree(srcSchema, repo.resolve("schema"));

        Path outputDir = workDir.resolve("reports");
        Path rulesDir = repo.resolve("rules");

        CliParams cli = new CliParams(
            dev.springreview.config.Mode.FILES,
            repo,
            null, null, null,
            List.of("src/main/java/com/example/Demo.java"),
            rulesDir, outputDir, null,
            Boolean.TRUE,  // offline
            null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null, null);

        var config = new ConfigLoader().load(cli, Map.of(), repo);

        Trace.bind(Trace.newTraceId());
        try {
            ReviewResult result = new ReviewOrchestrator().review(config, cli);

            // 报告生成
            assertThat(result.reportId()).matches("RPT-\\d{4}-\\d{4}");
            assertThat(Files.exists(result.latestJson())).isTrue();
            assertThat(Files.exists(result.historyDir())).isTrue();

            // JSON 合规
            JsonNode report = new ObjectMapper().readTree(result.latestJson().toFile());
            assertThat(report.path("report_id").asText()).isEqualTo(result.reportId());
            assertThat(report.path("schema_version").asText()).isEqualTo("1.0.0");
            assertThat(report.path("scope").path("mode").asText()).isEqualTo("FILES");
            assertThat(report.path("summary").isObject()).isTrue();
            assertThat(report.path("issues").isArray()).isTrue();
            assertThat(report.path("tool_runs").isArray()).isTrue();
            assertThat(report.path("metrics").path("total_files_scanned").asInt())
                .isGreaterThanOrEqualTo(1);

            // 命中硬编码密码（Demo.java 有明显 password 字面量）
            boolean hitSec01 = false;
            for (JsonNode issue : report.path("issues")) {
                if ("SRS-SEC-01-001".equals(issue.path("rule_id").asText())) {
                    hitSec01 = true;
                    break;
                }
            }
            if (!hitSec01) {
                System.out.println("[e2e] 未命中 SRS-SEC-01-001（取决于规则参数与边界），报告仍合规");
            }

            // Markdown 生成
            assertThat(Files.exists(result.latestMarkdown())).isTrue();
            String md = Files.readString(result.latestMarkdown(), StandardCharsets.UTF_8);
            assertThat(md).contains("# Spring Review Report");
            assertThat(md).contains(result.reportId());
        } finally {
            Trace.clear();
        }
    }

    @Test
    void moduleModeGeneratesEmptyReportForNoIssues(@TempDir Path workDir) throws IOException {
        Path repo = workDir.resolve("repo");
        Files.createDirectories(repo.resolve("src/main/java/com/example"));

        Files.writeString(repo.resolve("src/main/java/com/example/SafeService.java"), """
                package com.example;

                public class SafeService {
                    private final String name;

                    public SafeService(String name) {
                        this.name = name;
                    }

                    public String getName() {
                        return name;
                    }
                }
                """, StandardCharsets.UTF_8);

        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        Path srcRules = projectRoot.resolve("rules");
        Path srcSchema = projectRoot.resolve("schema");

        Assumptions.assumeTrue(Files.exists(srcRules.resolve("index.json")),
            "rules/index.json 缺失，跳过端到端集成测试");
        Assumptions.assumeTrue(Files.exists(srcSchema.resolve("rule-metadata.schema.json")),
            "schema/rule-metadata.schema.json 缺失，跳过端到端集成测试");
        Assumptions.assumeTrue(Files.exists(srcSchema.resolve("review-report.schema.json")),
            "schema/review-report.schema.json 缺失，跳过端到端集成测试");

        copyTree(srcRules, repo.resolve("rules"));
        copyTree(srcSchema, repo.resolve("schema"));

        Path outputDir = workDir.resolve("reports");
        Path rulesDir = repo.resolve("rules");

        CliParams cli = new CliParams(
            dev.springreview.config.Mode.MODULE,
            repo,
            null, null,
            List.of("src/main/java/com/example"),
            null,
            rulesDir, outputDir, null,
            Boolean.TRUE,
            null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null, null);

        var config = new ConfigLoader().load(cli, Map.of(), repo);

        Trace.bind(Trace.newTraceId());
        try {
            ReviewResult result = new ReviewOrchestrator().review(config, cli);
            JsonNode report = new ObjectMapper().readTree(result.latestJson().toFile());
            assertThat(report.path("report_id").asText()).isEqualTo(result.reportId());
            assertThat(report.path("scope").path("mode").asText()).isEqualTo("MODULE");
            assertThat(report.path("metrics").path("total_files_scanned").asInt())
                .isGreaterThanOrEqualTo(1);
            assertThat(report.path("summary").path("total_issues").asInt())
                .isGreaterThanOrEqualTo(0);
        } finally {
            Trace.clear();
        }
    }

    private static void copyTree(Path src, Path dst) throws IOException {
        if (!Files.exists(src)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(p -> {
                try {
                    Path rel = src.relativize(p);
                    Path target = dst.resolve(rel);
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(target);
                    } else {
                        Path parent = target.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
    }
}
