package dev.springreview.observability;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsTest {

    @Test
    void counterAccumulates() {
        Metrics m = new Metrics();
        m.counter("a");
        m.counter("a");
        m.counter("a", 3);
        Map<String, Long> snap = m.snapshotCounters();
        assertThat(snap).containsEntry("a", 5L);
    }

    @Test
    void timerAccumulates() {
        Metrics m = new Metrics();
        m.timer("x", 100);
        m.timer("x", 250);
        assertThat(m.snapshotTimers()).containsEntry("x", 350L);
    }

    @Test
    void ignoresInvalidInputs() {
        Metrics m = new Metrics();
        m.counter(null);
        m.counter("");
        m.timer(null, 10);
        m.timer("x", -1);
        assertThat(m.snapshotCounters()).isEmpty();
        assertThat(m.snapshotTimers()).isEmpty();
    }
}