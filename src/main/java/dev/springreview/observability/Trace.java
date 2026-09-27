package dev.springreview.observability;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 每次 run 一个 traceId；UUID v4 去连字符 32 位 hex。
 * 存于 MDC key = trace_id，供 logback pattern 读取。
 */
public final class Trace {

    public static final String MDC_KEY = "trace_id";

    private Trace() {
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void bind(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            return;
        }
        MDC.put(MDC_KEY, traceId);
    }

    public static String current() {
        String v = MDC.get(MDC_KEY);
        return v == null ? "" : v;
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}