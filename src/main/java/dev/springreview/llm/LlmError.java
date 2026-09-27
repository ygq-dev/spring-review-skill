package dev.springreview.llm;

public record LlmError(
    String code,
    String message,
    boolean retryable,
    Integer providerStatus,
    String traceId
) {

    public static final String NO_SECRET = "NO_SECRET";
    public static final String OFFLINE = "OFFLINE";
    public static final String HTTP_ERROR = "HTTP_ERROR";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String PARSE_ERROR = "PARSE_ERROR";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String INTERNAL = "INTERNAL";
}
