package dev.springreview.config;

import dev.springreview.exit.SpringReviewException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 配置合并：CLI > 环境变量 > .env.local > spring-review.yaml > 默认值。
 * - 环境变量使用 SPRING_REVIEW_* 前缀。
 * - .env.local 兼容短 key（如 LLM_MODEL、MODE、OFFLINE），自动映射到 SPRING_REVIEW_* 。
 * - YAML 以点号 key 浅层展开。
 */
public final class ConfigLoader {

    /**
     * .env.local 中的短 key → SPRING_REVIEW_* 别名。
     */
    private static final Map<String, String> DOTENV_ALIASES = Map.of(
        "MODE", "SPRING_REVIEW_MODE",
        "OFFLINE", "SPRING_REVIEW_OFFLINE",
        "LOG_LEVEL", "SPRING_REVIEW_LOG_LEVEL",
        "LLM_MODEL", "SPRING_REVIEW_LLM_MODEL",
        "LLM_BASE_URL", "SPRING_REVIEW_LLM_BASE_URL",
        "LLM_PROVIDER", "SPRING_REVIEW_LLM_PROVIDER"
    );

    public AppConfig load(CliParams cli, Map<String, String> env, Path workingDir) {
        Path configPath = cli.configPath();
        if (configPath == null) {
            String envConfig = env.get("SPRING_REVIEW_CONFIG");
            if (envConfig != null && !envConfig.isBlank()) {
                configPath = Path.of(envConfig);
            }
        }
        Map<String, String> yaml = loadYamlFlat(configPath);
        Map<String, String> dotenv = loadDotenv(workingDir);

        // review
        Mode mode = firstEnum(cli.mode(),
            envOrDotenv(env, dotenv, "SPRING_REVIEW_MODE", null),
            yaml.get("review.mode"), Mode.class, Mode.DIFF);
        Path repoRoot = firstPath(cli.repoRoot(),
            null, yaml.get("review.repoRoot"), workingDir);
        String baseRef = firstString(cli.baseRef(), null, yaml.get("review.baseRef"), "HEAD~1");
        String headRef = firstString(cli.headRef(), null, yaml.get("review.headRef"), "HEAD");
        List<String> modules = firstList(cli.modulePaths(), null, null, List.of());
        List<String> files = firstList(cli.filePaths(), null, null, List.of());

        // rules / schema / output
        Path rulesDir = firstPath(cli.rulesDir(), envOrDotenv(env, dotenv, "RULES_DIR", null),
            yaml.get("rules.dir"), workingDir.resolve("rules"));
        Path rulesIndex = firstPath(null, null, yaml.get("rules.index"), rulesDir.resolve("index.json"));
        Path schemaReport = firstPath(null, null, yaml.get("schema.reviewReport"),
            workingDir.resolve("schema/review-report.schema.json"));
        Path schemaRule = firstPath(null, null, yaml.get("schema.ruleMetadata"),
            workingDir.resolve("schema/rule-metadata.schema.json"));

        Path outputDir = firstPath(cli.outputDir(),
            envOrDotenv(env, dotenv, "SPRING_REVIEW_OUTPUT_DIR", null),
            yaml.get("output.dir"), workingDir.resolve("reports"));
        Path latestDir = firstPath(null, null, yaml.get("output.latestDir"), outputDir.resolve("latest"));
        String historyPattern = firstString(null, null, yaml.get("output.historyPattern"),
            "history/YYYY/MM/DD/<report_id>/");

        List<OutputFormat> formats = resolveFormats(cli, yaml);

        // llm
        boolean offlineCli = Boolean.TRUE.equals(cli.offline());
        boolean offlineEnv = "true".equalsIgnoreCase(
            envOrDotenv(env, dotenv, "SPRING_REVIEW_OFFLINE", null));
        boolean offline = offlineCli || offlineEnv;
        boolean noLlm = Boolean.TRUE.equals(cli.noLlm());
        boolean llmEnabled = !offline && !noLlm;
        String provider = firstString(cli.llmProvider(),
            envOrDotenv(env, dotenv, "SPRING_REVIEW_LLM_PROVIDER", null),
            yaml.get("llm.provider"), "deepseek");
        String model = firstString(cli.llmModel(),
            envOrDotenv(env, dotenv, "SPRING_REVIEW_LLM_MODEL", null),
            yaml.get("llm.model"), "deepseek-chat");
        String baseUrl = firstString(cli.llmBaseUrl(),
            envOrDotenv(env, dotenv, "SPRING_REVIEW_LLM_BASE_URL", null),
            yaml.get("llm.baseUrl"), "https://api.deepseek.com");
        long timeoutMs = firstLong(cli.llmTimeout(), null, yaml.get("llm.timeoutMs"), 60_000L);
        int maxConcurrency = firstInt(cli.maxConcurrency(), null, yaml.get("llm.maxConcurrency"), 4);
        double temperature = firstDouble(null, null, yaml.get("llm.temperature"), 0.1d);
        int maxTokens = firstInt(null, null, yaml.get("llm.maxTokens"), 4096);

        // static
        boolean checkstyle = firstBool(cli.enableCheckstyle(), null,
            yaml.get("static.checkstyle.enabled"), true);
        boolean pmd = firstBool(cli.enablePmd(), null, yaml.get("static.pmd.enabled"), true);
        boolean spotbugs = firstBool(cli.enableSpotbugs(), null,
            yaml.get("static.spotbugs.enabled"), false);

        // exit
        FailOn failOn = firstEnum(cli.failOn(), null, yaml.get("exit.failOn"),
            FailOn.class, FailOn.CRITICAL);
        boolean strict = firstBool(cli.strict(), null, yaml.get("exit.strict"), false);
        boolean validate = !Boolean.TRUE.equals(cli.noValidate());

        // logging
        String logLevel = firstString(cli.logLevel(),
            envOrDotenv(env, dotenv, "SPRING_REVIEW_LOG_LEVEL", null),
            yaml.get("logging.level"), "INFO");

        return new AppConfig(
            new AppConfig.Review(mode, repoRoot, baseRef, headRef, modules, files),
            new AppConfig.Rules(rulesDir, rulesIndex),
            new AppConfig.Schema(schemaReport, schemaRule),
            new AppConfig.Output(outputDir, latestDir, historyPattern, formats),
            new AppConfig.Llm(llmEnabled, provider, baseUrl, model, timeoutMs,
                maxConcurrency, temperature, maxTokens),
            new AppConfig.StaticTools(checkstyle, pmd, spotbugs),
            new AppConfig.Exit(failOn, strict),
            new AppConfig.Logging(logLevel),
            offline,
            validate,
            configPath
        );
    }

    // ---------- helpers ----------

    private static List<OutputFormat> resolveFormats(CliParams cli, Map<String, String> yaml) {
        boolean noMarkdown = Boolean.TRUE.equals(cli.noMarkdown());
        List<OutputFormat> fromCli = cli.formats();
        List<OutputFormat> base;
        if (fromCli != null && !fromCli.isEmpty()) {
            base = new ArrayList<>(fromCli);
        } else {
            base = new ArrayList<>(List.of(OutputFormat.MARKDOWN, OutputFormat.JSON));
        }
        if (noMarkdown) {
            base.remove(OutputFormat.MARKDOWN);
        }
        return Collections.unmodifiableList(base);
    }

    private static String envOrDotenv(Map<String, String> env, Map<String, String> dotenv,
                                      String envKey, String fallback) {
        String v = env.get(envKey);
        if (v != null && !v.isEmpty()) {
            return v;
        }
        v = dotenv.get(envKey);
        if (v != null && !v.isEmpty()) {
            return v;
        }
        return fallback;
    }

    private static String firstString(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static Path firstPath(Path cliValue, String envValue, String yamlValue, Path fallback) {
        if (cliValue != null) {
            return cliValue.toAbsolutePath().normalize();
        }
        if (envValue != null && !envValue.isBlank()) {
            return Path.of(envValue).toAbsolutePath().normalize();
        }
        if (yamlValue != null && !yamlValue.isBlank()) {
            return Path.of(yamlValue).toAbsolutePath().normalize();
        }
        return fallback == null ? null : fallback.toAbsolutePath().normalize();
    }

    private static List<String> firstList(List<String> cliValue, List<String> envValue,
                                          List<String> yamlValue, List<String> fallback) {
        if (cliValue != null && !cliValue.isEmpty()) {
            return List.copyOf(cliValue);
        }
        if (envValue != null && !envValue.isEmpty()) {
            return List.copyOf(envValue);
        }
        if (yamlValue != null && !yamlValue.isEmpty()) {
            return List.copyOf(yamlValue);
        }
        return fallback == null ? List.of() : List.copyOf(fallback);
    }

    private static boolean firstBool(Boolean cliValue, Boolean envValue,
                                     String yamlValue, boolean fallback) {
        if (cliValue != null) {
            return cliValue;
        }
        if (envValue != null) {
            return envValue;
        }
        if (yamlValue != null && !yamlValue.isBlank()) {
            return Boolean.parseBoolean(yamlValue.trim());
        }
        return fallback;
    }

    private static long firstLong(Long cliValue, Long envValue, String yamlValue, long fallback) {
        if (cliValue != null) {
            return cliValue;
        }
        if (envValue != null) {
            return envValue;
        }
        if (yamlValue != null && !yamlValue.isBlank()) {
            try {
                return Long.parseLong(yamlValue.trim());
            } catch (NumberFormatException ignored) {
                // 交给默认值
            }
        }
        return fallback;
    }

    private static int firstInt(Integer cliValue, Integer envValue, String yamlValue, int fallback) {
        if (cliValue != null) {
            return cliValue;
        }
        if (envValue != null) {
            return envValue;
        }
        if (yamlValue != null && !yamlValue.isBlank()) {
            try {
                return Integer.parseInt(yamlValue.trim());
            } catch (NumberFormatException ignored) {
                // 交给默认值
            }
        }
        return fallback;
    }

    private static double firstDouble(Double cliValue, Double envValue,
                                      String yamlValue, double fallback) {
        if (cliValue != null) {
            return cliValue;
        }
        if (envValue != null) {
            return envValue;
        }
        if (yamlValue != null && !yamlValue.isBlank()) {
            try {
                return Double.parseDouble(yamlValue.trim());
            } catch (NumberFormatException ignored) {
                // 交给默认值
            }
        }
        return fallback;
    }

    private static <E extends Enum<E>> E firstEnum(E cliValue, String envValue, String yamlValue,
                                                   Class<E> type, E fallback) {
        if (cliValue != null) {
            return cliValue;
        }
        E fromEnv = tryParseEnum(envValue, type);
        if (fromEnv != null) {
            return fromEnv;
        }
        E fromYaml = tryParseEnum(yamlValue, type);
        return fromYaml != null ? fromYaml : fallback;
    }

    private static <E extends Enum<E>> E tryParseEnum(String value, Class<E> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    // ---------- IO ----------

    private Map<String, String> loadYamlFlat(Path configPath) {
        if (configPath == null) {
            return Map.of();
        }
        Path p = configPath;
        if (!Files.exists(p)) {
            Path fallback = p.getFileName() != null
                ? p.resolveSibling("." + p.getFileName())
                : null;
            if (fallback != null && Files.exists(fallback)) {
                p = fallback;
            } else {
                return Map.of();
            }
        }
        try (InputStream in = Files.newInputStream(p)) {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Object root = yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (!(root instanceof Map<?, ?> map)) {
                return Map.of();
            }
            Map<String, String> flat = new LinkedHashMap<>();
            flatten(map, "", flat);
            return flat;
        } catch (IOException ex) {
            throw SpringReviewException.cli("无法读取配置文件: " + p, ex);
        } catch (RuntimeException ex) {
            throw SpringReviewException.cli("配置文件格式错误: " + p, ex);
        }
    }

    private void flatten(Map<?, ?> map, String prefix, Map<String, String> out) {
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = String.valueOf(e.getKey());
            String full = prefix.isEmpty() ? key : prefix + "." + key;
            Object v = e.getValue();
            if (v instanceof Map<?, ?> child) {
                flatten(child, full, out);
            } else if (v instanceof Iterable<?> it) {
                StringBuilder sb = new StringBuilder();
                for (Object item : it) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(String.valueOf(item));
                }
                out.put(full, sb.toString());
            } else if (v != null) {
                out.put(full, String.valueOf(v));
            }
        }
    }

    private Map<String, String> loadDotenv(Path workingDir) {
        Path envFile = workingDir.resolve(".env.local");
        if (!Files.exists(envFile)) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        try {
            for (String raw : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                if (v.length() >= 2 && (v.startsWith("\"") && v.endsWith("\"")
                    || v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
                out.put(k, v);
                String alias = DOTENV_ALIASES.get(k);
                if (alias != null) {
                    out.put(alias, v);
                }
            }
        } catch (IOException ex) {
            throw SpringReviewException.cli(".env.local 读取失败: " + envFile, ex);
        }
        return out;
    }
}
