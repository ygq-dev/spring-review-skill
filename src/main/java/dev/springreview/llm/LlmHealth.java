package dev.springreview.llm;

import java.time.Instant;

public record LlmHealth(
    String status,
    String provider,
    String model,
    long latencyMs,
    String error,
    Instant checkedAt,
    String traceId
) {

    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
    public static final String DEGRADED = "DEGRADED";
    public static final String OFFLINE = "OFFLINE";
}
