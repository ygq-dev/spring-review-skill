package dev.springreview.llm;

import com.sun.net.httpserver.HttpServer;
import dev.springreview.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatClientTest {

    private static AppConfig.Llm llm(String baseUrl) {
        return new AppConfig.Llm(true, "deepseek", baseUrl, "test-model",
            5000, 1, 0.1, 100);
    }

    @Test
    void successResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/chat/completions", exchange -> {
            String resp = "{\"choices\":[{\"message\":{\"content\":\"hello\"},"
                + "\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":3,\"total_tokens\":8}}";
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        try {
            OpenAiCompatClient client = new OpenAiCompatClient(
                llm("http://127.0.0.1:" + port), "fake-key");
            LlmRequest req = LlmRequest.simple(List.of(Message.user("hi")), "trace-1");
            LlmResponse resp = client.complete(req);
            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.content()).isEqualTo("hello");
            assertThat(resp.finishReason()).isEqualTo("stop");
            assertThat(resp.usage().totalTokens()).isEqualTo(8);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rateLimitedRetriesThenFails() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/chat/completions", exchange -> {
            hits.incrementAndGet();
            byte[] body = "rate limited".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(429, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            OpenAiCompatClient client = new OpenAiCompatClient(
                llm("http://127.0.0.1:" + port), "fake-key");
            LlmRequest req = LlmRequest.simple(List.of(Message.user("hi")), "t");
            LlmResponse resp = client.complete(req);
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.error().code()).isEqualTo(LlmError.RATE_LIMITED);
            assertThat(hits.get()).isGreaterThanOrEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void nonRetryableStatusFailsImmediately() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/chat/completions", exchange -> {
            hits.incrementAndGet();
            byte[] body = "forbidden".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            OpenAiCompatClient client = new OpenAiCompatClient(
                llm("http://127.0.0.1:" + port), "fake-key");
            LlmResponse resp = client.complete(
                LlmRequest.simple(List.of(Message.user("hi")), "t"));
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.error().code()).isEqualTo(LlmError.HTTP_ERROR);
            assertThat(resp.error().retryable()).isFalse();
            assertThat(hits.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void malformedResponseYieldsParseError() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/chat/completions", exchange -> {
            byte[] body = "not-json".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            OpenAiCompatClient client = new OpenAiCompatClient(
                llm("http://127.0.0.1:" + port), "fake-key");
            LlmResponse resp = client.complete(
                LlmRequest.simple(List.of(Message.user("hi")), "t"));
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.error().code()).isEqualTo(LlmError.PARSE_ERROR);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void factoryReturnsNoopWhenDisabled() {
        AppConfig.Llm disabled = new AppConfig.Llm(false, "deepseek",
            "https://api.deepseek.com", "deepseek-chat", 1000, 1, 0.1, 100);
        LlmClient client = LlmClientFactory.create(disabled, Path.of("."), Map.of());
        assertThat(client.enabled()).isFalse();
        assertThat(client.complete(LlmRequest.simple(
            List.of(Message.user("hi")), "t")).error().code())
            .isEqualTo(LlmError.OFFLINE);
    }

    @Test
    void factoryReturnsNoopWhenNoKey() {
        AppConfig.Llm enabled = new AppConfig.Llm(true, "deepseek",
            "https://api.deepseek.com", "deepseek-chat", 1000, 1, 0.1, 100);
        LlmClient client = LlmClientFactory.create(enabled, Path.of("."), Map.of());
        assertThat(client.enabled()).isFalse();
        assertThat(client.complete(LlmRequest.simple(
            List.of(Message.user("hi")), "t")).error().code())
            .isEqualTo(LlmError.NO_SECRET);
    }

    @Test
    void factoryUsesEnvKey() {
        AppConfig.Llm enabled = new AppConfig.Llm(true, "deepseek",
            "https://api.deepseek.com", "deepseek-chat", 1000, 1, 0.1, 100);
        LlmClient client = LlmClientFactory.create(enabled, Path.of("."),
            Map.of("DEEPSEEK_API_KEY", "key-123"));
        assertThat(client.enabled()).isTrue();
        assertThat(client.provider()).isEqualTo("deepseek");
    }
}
