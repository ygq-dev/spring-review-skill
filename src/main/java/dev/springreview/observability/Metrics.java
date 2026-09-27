package dev.springreview.observability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 内存指标注册表；线程安全；不写目标仓库。
 */
public final class Metrics {

    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> timers = new ConcurrentHashMap<>();

    public void counter(String name) {
        counter(name, 1L);
    }

    public void counter(String name, long delta) {
        if (name == null || name.isEmpty()) {
            return;
        }
        counters.computeIfAbsent(name, k -> new LongAdder()).add(delta);
    }

    public void timer(String name, long millis) {
        if (name == null || name.isEmpty() || millis < 0) {
            return;
        }
        timers.computeIfAbsent(name, k -> new LongAdder()).add(millis);
    }

    public Map<String, Long> snapshotCounters() {
        Map<String, Long> out = new LinkedHashMap<>();
        counters.forEach((k, v) -> out.put(k, v.sum()));
        return out;
    }

    public Map<String, Long> snapshotTimers() {
        Map<String, Long> out = new LinkedHashMap<>();
        timers.forEach((k, v) -> out.put(k, v.sum()));
        return out;
    }
}