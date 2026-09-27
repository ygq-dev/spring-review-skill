package dev.springreview.llm;

import java.util.List;

public record LlmRequest(
    String provider,
    String model,
    List<Message> messages,
    Double temperature,
    Integer maxTokens,
    ResponseFormat responseFormat,
    Long timeoutMs,
    String traceId
) {

    public enum ResponseFormat {TEXT, JSON_OBJECT}

    public static LlmRequest simple(List<Message> messages, String traceId) {
        return new LlmRequest(null, null, messages, null, null,
            ResponseFormat.TEXT, null, traceId);
    }
}
