package dev.springreview.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.springreview.config.AppConfig;
import dev.springreview.observability.Logs;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * OpenAI 兼容 HTTP 客户端。
 * - POST {baseUrl}/chat/completions
 * - Bearer 鉴权
 * - 重试 3 次尝试；指数退避；429 尊重退避
 * - 并发由 Semaphore 控制
 * - 不缓存 prompt/response
 */
public final class OpenAiCompatClient implements LlmClient {

    private static final Logger LOG = Logs.logger("llm");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 500L;
    private static final long MAX_BACKOFF_MS = 5_000L;

    private final AppConfig.Llm config;
    private final String apiKey;
    private final HttpClient http;
    private final Semaphore semaphore;

    public OpenAiCompatClient(AppConfig.Llm config, String apiKey) {
        this.config = config;
        this.apiKey = apiKey;
        long connectTimeout = Math.min(10_000L, Math.max(1_000L, config.timeoutMs()));
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(connectTimeout))
            .build();
        this.semaphore = new Semaphore(Math.max(1, config.maxConcurrency()));
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        Logs.module("M11");
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            return LlmResponse.error(new LlmError(
                LlmError.INVALID_REQUEST, "messages is empty", false, null,
                request == null ? null : request.traceId()), 0);
        }
        long start = System.currentTimeMillis();
        try {
            semaphore.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return LlmResponse.error(new LlmError(
                LlmError.INTERNAL, "interrupted", false, null,
                request.traceId()), System.currentTimeMillis() - start);
        }
        try {
            return doComplete(request, start);
        } finally {
            semaphore.release();
        }
    }

    private LlmResponse doComplete(LlmRequest req, long start) {
        String url = resolveUrl();
        String model = (req.model() != null && !req.model().isBlank())
            ? req.model() : config.model();
        double temperature = req.temperature() != null
            ? req.temperature() : config.temperature();
        int maxTokens = req.maxTokens() != null
            ? req.maxTokens() : config.maxTokens();
        long timeoutMs = req.timeoutMs() != null
            ? req.timeoutMs() : config.timeoutMs();

        String body = buildBody(req, model, temperature, maxTokens);
        LlmError lastError = null;
        long backoff = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long attemptStart = System.currentTimeMillis();
            try {
                HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
                HttpResponse<String> resp = http.send(httpReq,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                long latency = System.currentTimeMillis() - start;
                int code = resp.statusCode();

                if (code == 200) {
                    return parseSuccess(resp.body(), latency, req.traceId());
                }
                if (isRetryableStatus(code)) {
                    lastError = new LlmError(
                        code == 429 ? LlmError.RATE_LIMITED : LlmError.HTTP_ERROR,
                        "HTTP " + code, true, code, req.traceId());
                    LOG.warn("llm http retryable status={} attempt={} latencyMs={}",
                        code, attempt, latency);
                    if (attempt < MAX_ATTEMPTS) {
                        sleepQuietly(backoff);
                        backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                        continue;
                    }
                    return LlmResponse.error(lastError, latency);
                }
                return LlmResponse.error(new LlmError(
                    LlmError.HTTP_ERROR, "HTTP " + code, false, code,
                    req.traceId()), latency);
            } catch (HttpTimeoutException ex) {
                long latency = System.currentTimeMillis() - start;
                lastError = new LlmError(LlmError.TIMEOUT, "timeout", true, null, req.traceId());
                LOG.warn("llm timeout attempt={} latencyMs={}", attempt, latency);
                if (attempt < MAX_ATTEMPTS) {
                    sleepQuietly(backoff);
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                    continue;
                }
                return LlmResponse.error(lastError, latency);
            } catch (IOException ex) {
                long latency = System.currentTimeMillis() - start;
                lastError = new LlmError(LlmError.HTTP_ERROR,
                    ex.getMessage(), true, null, req.traceId());
                LOG.warn("llm io error attempt={} msg={}", attempt, ex.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleepQuietly(backoff);
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                    continue;
                }
                return LlmResponse.error(lastError, latency);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return LlmResponse.error(new LlmError(
                    LlmError.INTERNAL, "interrupted", false, null,
                    req.traceId()), System.currentTimeMillis() - start);
            } catch (RuntimeException ex) {
                return LlmResponse.error(new LlmError(
                    LlmError.INTERNAL, ex.getMessage(), false, null,
                    req.traceId()), System.currentTimeMillis() - start);
            } finally {
                long attemptLatency = System.currentTimeMillis() - attemptStart;
                LOG.debug("llm attempt={} latencyMs={}", attempt, attemptLatency);
            }
        }
        return LlmResponse.error(lastError != null ? lastError
                : new LlmError(LlmError.INTERNAL, "exhausted", false, null, req.traceId()),
            System.currentTimeMillis() - start);
    }

    private String resolveUrl() {
        String base = config.baseUrl();
        if (base == null || base.isBlank()) {
            base = "https://api.deepseek.com";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/chat/completions";
    }

    private String buildBody(LlmRequest req, String model, double temperature, int maxTokens) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        ArrayNode msgs = body.putArray("messages");
        for (Message m : req.messages()) {
            ObjectNode mn = msgs.addObject();
            mn.put("role", m.role());
            mn.put("content", m.content());
        }
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        if (req.responseFormat() == LlmRequest.ResponseFormat.JSON_OBJECT) {
            ObjectNode rf = body.putObject("response_format");
            rf.put("type", "json_object");
        }
        return body.toString();
    }

    private LlmResponse parseSuccess(String json, long latency, String traceId) {
        try {
            JsonNode n = MAPPER.readTree(json);
            JsonNode choices = n.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return LlmResponse.error(new LlmError(
                    LlmError.PARSE_ERROR, "no choices", false, 200,
                    traceId), latency);
            }
            JsonNode c0 = choices.get(0);
            String content = c0.path("message").path("content").asText("");
            String finish = c0.path("finish_reason").asText(null);
            JsonNode u = n.path("usage");
            long in = u.path("prompt_tokens").asLong(0);
            long out = u.path("completion_tokens").asLong(0);
            long tot = u.path("total_tokens").asLong(in + out);
            return LlmResponse.success(content,
                new LlmResponse.Usage(in, out, tot),
                finish, latency);
        } catch (IOException ex) {
            return LlmResponse.error(new LlmError(
                LlmError.PARSE_ERROR, ex.getMessage(), false, 200,
                traceId), latency);
        }
    }

    private static boolean isRetryableStatus(int code) {
        return code == 408 || code == 429 || (code >= 500 && code < 600);
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public LlmHealth health() {
        return new LlmHealth(LlmHealth.UP, provider(), config.model(),
            0, null, Instant.now(), "");
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String provider() {
        return config.provider();
    }

    @SuppressWarnings("unused")
    private static List<Message> unused() {
        return List.of();
    }
}
