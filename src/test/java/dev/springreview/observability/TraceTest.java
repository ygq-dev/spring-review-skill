package dev.springreview.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceTest {

    @AfterEach
    void cleanup() {
        Trace.clear();
    }

    @Test
    void newTraceId_is32Hex() {
        String id = Trace.newTraceId();
        assertThat(id).hasSize(32).matches("[0-9a-f]{32}");
    }

    @Test
    void bindAndCurrent() {
        Trace.bind("abc");
        assertThat(Trace.current()).isEqualTo("abc");
    }

    @Test
    void current_returnsEmptyWhenUnbound() {
        assertThat(Trace.current()).isEmpty();
    }

    @Test
    void bindIgnoresNullOrEmpty() {
        Trace.bind(null);
        Trace.bind("");
        assertThat(Trace.current()).isEmpty();
    }
}