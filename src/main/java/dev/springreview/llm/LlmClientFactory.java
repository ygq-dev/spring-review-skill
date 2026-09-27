package dev.springreview.llm;

import dev.springreview.config.AppConfig;
import dev.springreview.observability.Logs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据 AppConfig 与环境决定创建哪种 LlmClient。
 * - 未启用 → Noop(OFFLINE)
 * - 无密钥 → Noop(NO_SECRET)
 * - 否则 → OpenAiCompatClient
 */
public final class LlmClientFactory {

    private LlmClientFactory() {
    }

    public static LlmClient create(AppConfig config, Path workingDir) {
        return create(config.llm(), workingDir, System.getenv());
    }

    public static LlmClient create(AppConfig.Llm llm, Path workingDir,
                                   Map<String, String> env) {
        Logs.module("M11");
        if (llm == null || !llm.enabled()) {
            return new NoopLlmClient(llm == null ? "" : llm.provider(), LlmError.OFFLINE);
        }
        String key = resolveApiKey(llm.provider(), workingDir, env);
        if (key == null || key.isBlank()) {
            return new NoopLlmClient(llm.provider(), LlmError.NO_SECRET);
        }
        return new OpenAiCompatClient(llm, key);
    }

    private static String resolveApiKey(String provider, Path workingDir,
                                        Map<String, String> env) {
        List<String> candidates = new ArrayList<>();
        String p = provider == null ? "" : provider.toLowerCase();
        switch (p) {
            case "deepseek" -> candidates.add("DEEPSEEK_API_KEY");
            case "openai" -> candidates.add("OPENAI_API_KEY");
            case "qwen" -> {
                candidates.add("DASHSCOPE_API_KEY");
                candidates.add("QWEN_API_KEY");
            }
            default -> {
                // 自定义 provider 依赖 LLM_API_KEY
            }
        }
        candidates.add("LLM_API_KEY");

        if (env != null) {
            for (String k : candidates) {
                String v = env.get(k);
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        Map<String, String> dotenv = readDotenv(workingDir);
        for (String k : candidates) {
            String v = dotenv.get(k);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static Map<String, String> readDotenv(Path workingDir) {
        if (workingDir == null) {
            return Map.of();
        }
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
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return out;
    }
}
