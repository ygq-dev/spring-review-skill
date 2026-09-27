package dev.springreview.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 日志入口，包装 SLF4J；提供 module / event 结构化绑定。
 */
public final class Logs {

    private static final String MDC_MODULE = "module";
    private static final String MDC_EVENT = "event";

    private Logs() {
    }

    public static Logger logger(String name) {
        return LoggerFactory.getLogger(name);
    }

    public static void module(String module) {
        MDC.put(MDC_MODULE, module);
    }

    public static void event(String event) {
        MDC.put(MDC_EVENT, event);
    }

    public static void clear() {
        MDC.remove(MDC_MODULE);
        MDC.remove(MDC_EVENT);
    }
}