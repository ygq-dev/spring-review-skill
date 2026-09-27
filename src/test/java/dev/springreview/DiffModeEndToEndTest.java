package dev.springreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springreview.config.CliParams;
import dev.springreview.config.ConfigLoader;
import dev.springreview.config.Mode;
import dev.springreview.observability.Trace;
import dev.springreview.orchestration.ReviewOrchestrator;
import dev.springreview.orchestration.ReviewResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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
 * DIFF 模式端到端测试。
 * 使用临时 Git 仓库（@TempDir 下创建），不涉及用户项目仓库。
 * 只读审查 Skill 自身；对目标仓库保持只读。
 */
class DiffModeEndToEndTest {

    // ---------- 测试 1：修改文件应被收集 ----------

    @Test
    void diffModeReportsModifiedFile(@TempDir Path workDir) throws Exception {
        Path repo = setupRepoModified(workDir);
        prepareRulesAndSchema(repo);

        Path outputDir = workDir.resolve("reports");
        CliParams cli = cliDiff(repo, repo.resolve("rules"), outputDir);
        var config = new ConfigLoader().load(cli, Map.of(), repo);

        Trace.bind(Trace.newTraceId());
        try {
            ReviewResult result = new ReviewOrchestrator().review(config, cli);

            assertThat(result.reportId()).matches("RPT-\\d{4}-\\d{4}");
            assertThat(Files.exists(result.latestJson())).isTrue();

            JsonNode report = new ObjectMapper().readTree(result.latestJson().toFile());
            assertThat(report.path("scope").path("mode").asText()).isEqualTo("DIFF");
            assertThat(report.path("scope").path("base_ref").asText()).isEqualTo("HEAD~1");
            assertThat(report.path("scope").path("head_ref").asText()).isEqualTo("HEAD");
            assertThat(report.path("scope").path("diff_lines").asInt()).isGreaterThan(0);
            assertThat(report.path("scope").path("paths").isArray()).isTrue();
            assertThat(report.path("scope").path("paths").size()).isGreaterThanOrEqualTo(1);

            // 断言报告结构合规
            assertThat(report.path("summary").path("total_issues").asInt()).isGreaterThanOrEqualTo(0);
            assertThat(report.path("tool_runs").isArray()).isTrue();
            assertThat(report.path("metrics").path("total_files_scanned").asInt()).isGreaterThanOrEqualTo(1);

            // Markdown 生成
            assertThat(Files.exists(result.latestMarkdown())).isTrue();
            String md = Files.readString(result.latestMarkdown(), StandardCharsets.UTF_8);
            assertThat(md).contains("mode: `DIFF`");
        } finally {
            Trace.clear();
        }
    }

    // ---------- 测试 2：DIFF 只收集变更文件 ----------

    @Test
    void diffModeOnlyIncludesChangedFile(@TempDir Path workDir) throws Exception {
        Path repo = setupRepoTwoFilesOneChange(workDir);
        prepareRulesAndSchema(repo);

        Path outputDir = workDir.resolve("reports");
        CliParams cli = cliDiff(repo, repo.resolve("rules"), outputDir);
        var config = new ConfigLoader().load(cli, Map.of(), repo);

        Trace.bind(Trace.newTraceId());
        try {
            ReviewResult result = new ReviewOrchestrator().review(config, cli);
            JsonNode report = new ObjectMapper().readTree(result.latestJson().toFile());
            JsonNode paths = report.path("scope").path("paths");

            boolean hasA = false;
            boolean hasB = false;
            for (JsonNode p : paths) {
                String s = p.asText();
                if (s.endsWith("A.java")) {
                    hasA = true;
                }
                if (s.endsWith("B.java")) {
                    hasB = true;
                }
            }
            assertThat(hasA).as("变更文件 A.java 应被收集").isTrue();
            assertThat(hasB).as("未变更的 B.java 不应被收集").isFalse();
        } finally {
            Trace.clear();
        }
    }

    // ---------- 测试 3：新增文件应被收集 ----------

    @Test
    void diffModeHandlesNewFile(@TempDir Path workDir) throws Exception {
        Path repo = setupRepoWithAddition(workDir);
        prepareRulesAndSchema(repo);

        Path outputDir = workDir.resolve("reports");
        CliParams cli = cliDiff(repo, repo.resolve("rules"), outputDir);
        var config = new ConfigLoader().load(cli, Map.of(), repo);

        Trace.bind(Trace.newTraceId());
        try {
            ReviewResult result = new ReviewOrchestrator().review(config, cli);
            JsonNode report = new ObjectMapper().readTree(result.latestJson().toFile());
            JsonNode paths = report.path("scope").path("paths");

            boolean hasB = false;
            for (JsonNode p : paths) {
                if (p.asText().endsWith("B.java")) {
                    hasB = true;
                }
            }
            assertThat(hasB).as("新增的 B.java 应被收集").isTrue();
            assertThat(report.path("scope").path("diff_lines").asInt()).isGreaterThan(0);
        } finally {
            Trace.clear();
        }
    }

    // ---------- helpers ----------

    private static CliParams cliDiff(Path repo, Path rulesDir, Path outputDir) {
        return new CliParams(
            Mode.DIFF, repo, "HEAD~1", "HEAD", null, null,
            rulesDir, outputDir, null,
            Boolean.TRUE,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null);
    }

    private static void prepareRulesAndSchema(Path repo) throws IOException {
        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        Path srcRules = projectRoot.resolve("rules");
        Path srcSchema = projectRoot.resolve("schema");

        Assumptions.assumeTrue(Files.exists(srcRules.resolve("index.json")),
            "rules/index.json 缺失，跳过 DIFF 端到端测试");
        Assumptions.assumeTrue(Files.exists(srcSchema.resolve("rule-metadata.schema.json")),
            "schema/rule-metadata.schema.json 缺失，跳过");
        Assumptions.assumeTrue(Files.exists(srcSchema.resolve("review-report.schema.json")),
            "schema/review-report.schema.json 缺失，跳过");

        copyTree(srcRules, repo.resolve("rules"));
        copyTree(srcSchema, repo.resolve("schema"));
    }

    /** 一次修改：初始 commit 无违规，第二次 commit 加入硬编码密码。 */
    private static Path setupRepoModified(Path workDir) throws IOException, GitAPIException {
        Path repo = workDir.resolve("repo");
        Files.createDirectories(repo);
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            Path fileA = repo.resolve("src/main/java/com/example/Demo.java");
            Files.createDirectories(fileA.getParent());

            Files.writeString(fileA, """
                    package com.example;
                    public class Demo {
                        private final String name = "hello";
                    }
                    """, StandardCharsets.UTF_8);
            commit(git, "init");

            Files.writeString(fileA, """
                    package com.example;
                    public class Demo {
                        private final String name = "hello";
                        private final String password = "admin123";
                    }
                    """, StandardCharsets.UTF_8);
            commit(git, "bad");
        }
        return repo;
    }

    /** 两个文件，只改 A.java。 */
    private static Path setupRepoTwoFilesOneChange(Path workDir) throws IOException, GitAPIException {
        Path repo = workDir.resolve("repo");
        Files.createDirectories(repo);
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            Path fileA = repo.resolve("src/main/java/com/example/A.java");
            Path fileB = repo.resolve("src/main/java/com/example/B.java");
            Files.createDirectories(fileA.getParent());

            Files.writeString(fileA, """
                    package com.example;
                    public class A {
                        private final String name = "hello";
                    }
                    """, StandardCharsets.UTF_8);
            Files.writeString(fileB, """
                    package com.example;
                    public class B {
                        private final String name = "world";
                    }
                    """, StandardCharsets.UTF_8);
            commit(git, "init");

            Files.writeString(fileA, """
                    package com.example;
                    public class A {
                        private final String name = "hello";
                        private final String password = "admin123";
                    }
                    """, StandardCharsets.UTF_8);
            commit(git, "bad A");
        }
        return repo;
    }

    /** 新增 B.java。 */
    private static Path setupRepoWithAddition(Path workDir) throws IOException, GitAPIException {
        Path repo = workDir.resolve("repo");
        Files.createDirectories(repo);
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            Path fileA = repo.resolve("src/main/java/com/example/A.java");
            Files.createDirectories(fileA.getParent());
            Files.writeString(fileA, """
                    package com.example;
                    public class A {
                        private final String name = "hello";
                    }
                    """, StandardCharsets.UTF_8);
            commit(git, "init");

            Path fileB = repo.resolve("src/main/java/com/example/B.java");
            Files.writeString(fileB, """
                    package com.example;
                    public class B {
                        private final String password = "admin123";
                    }
                    """, StandardCharsets.UTF_8);
            commit(git, "add B");
        }
        return repo;
    }

    private static void commit(Git git, String message) throws GitAPIException {
        git.add().addFilepattern("src").call();
        git.commit()
            .setMessage(message)
            .setAuthor("test", "test@example.com")
            .setCommitter("test", "test@example.com")
            .call();
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
