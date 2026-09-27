package dev.springreview.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    private static CliParams emptyCli() {
        return new CliParams(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    @Test
    void defaultsApplied(@TempDir Path dir) {
        AppConfig c = loader.load(emptyCli(), Map.of(), dir);
        assertThat(c.review().mode()).isEqualTo(Mode.DIFF);
        assertThat(c.review().baseRef()).isEqualTo("HEAD~1");
        assertThat(c.review().headRef()).isEqualTo("HEAD");
        assertThat(c.llm().provider()).isEqualTo("deepseek");
        assertThat(c.llm().model()).isEqualTo("deepseek-chat");
        assertThat(c.exit().failOn()).isEqualTo(FailOn.CRITICAL);
        assertThat(c.staticTools().checkstyle()).isTrue();
        assertThat(c.staticTools().pmd()).isTrue();
        assertThat(c.staticTools().spotbugs()).isFalse();
        assertThat(c.logging().level()).isEqualTo("INFO");
    }

    @Test
    void cliOverridesEnvAndYaml(@TempDir Path dir) throws IOException {
        writeYaml(dir, """
                review:
                  mode: files
                  baseRef: yaml-base
                llm:
                  model: yaml-model
                """);
        Map<String, String> env = Map.of(
                "SPRING_REVIEW_MODE", "MODULE",
                "SPRING_REVIEW_LLM_MODEL", "env-model");

        CliParams cli = new CliParams(
                Mode.DIFF, null, "cli-base", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, "cli-model", null, null, null);

        AppConfig c = loader.load(cli, env, dir);
        assertThat(c.review().mode()).isEqualTo(Mode.DIFF);
        assertThat(c.review().baseRef()).isEqualTo("cli-base");
        assertThat(c.llm().model()).isEqualTo("cli-model");
    }

    @Test
    void envOverridesYaml_whenNoCli(@TempDir Path dir) throws IOException {
        writeYaml(dir, """
                llm:
                  model: yaml-model
                """);
        Map<String, String> env = Map.of("SPRING_REVIEW_LLM_MODEL", "env-model");
        AppConfig c = loader.load(emptyCli(), env, dir);
        assertThat(c.llm().model()).isEqualTo("env-model");
    }

    @Test
    void dotenvUsedWhenNoEnvOrCli(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".env.local"), "LLM_MODEL=dotenv-model\n", StandardCharsets.UTF_8);
        AppConfig c = loader.load(emptyCli(), Map.of(), dir);
        assertThat(c.llm().model()).isEqualTo("dotenv-model");
    }

    @Test
    void envOverridesDotenv(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".env.local"), "LLM_MODEL=dotenv-model\n", StandardCharsets.UTF_8);
        Map<String, String> env = Map.of("SPRING_REVIEW_LLM_MODEL", "env-model");
        AppConfig c = loader.load(emptyCli(), env, dir);
        assertThat(c.llm().model()).isEqualTo("env-model");
    }

    @Test
    void offlineForcesLlmDisabled(@TempDir Path dir) {
        CliParams cli = new CliParams(
                null, null, null, null, null, null, null, null, null,
                Boolean.TRUE, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
        AppConfig c = loader.load(cli, Map.of(), dir);
        assertThat(c.offline()).isTrue();
        assertThat(c.llm().enabled()).isFalse();
    }

    @Test
    void noMarkdownRemovesMarkdownFormat(@TempDir Path dir) {
        CliParams cli = new CliParams(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, Boolean.TRUE,
                null, null, null, null, null);
        AppConfig c = loader.load(cli, Map.of(), dir);
        assertThat(c.output().formats()).containsExactly(OutputFormat.JSON);
    }

    private static void writeYaml(Path dir, String content) throws IOException {
        Files.writeString(dir.resolve("spring-review.yaml"), content, StandardCharsets.UTF_8);
    }
}