package dev.springreview.llm;

public record LlmResponse(
    String content,
    Usage usage,
    String finishReason,
    long latencyMs,
    LlmError error
) {

    public record Usage(long inputTokens, long outputTokens, long totalTokens) {

        public static Usage zero() {
            return new Usage(0, 0, 0);
        }

        public Usage plus(Usage other) {
            if (other == null) {
                return this;
            }
            return new Usage(
                inputTokens + other.inputTokens,
                outputTokens + other.outputTokens,
                totalTokens + other.totalTokens);
        }
    }

    public boolean isSuccess() {
        return error == null;
    }

    public static LlmResponse success(String content, Usage usage,
                                      String finishReason, long latencyMs) {
        return new LlmResponse(content, usage, finishReason, latencyMs, null);
    }

    public static LlmResponse error(LlmError error, long latencyMs) {
        return new LlmResponse(null, Usage.zero(), null, latencyMs, error);
    }
}
