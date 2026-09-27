package dev.springreview.orchestration;

import dev.springreview.config.FailOn;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewResultTest {

    private static ReviewResult r(Map<String, Integer> sev, int total) {
        return new ReviewResult(
                "r", "t", Path.of("j"), Path.of("m"), Path.of("h"),
                total, sev, 0, 0, Map.of(), 1.0, true, FailOn.CRITICAL);
    }

    @Test
    void neverIsFalse() {
        ReviewResult x = r(Map.of("BLOCKER", 5), 5);
        assertThat(x.reachedFailOn(FailOn.NEVER)).isFalse();
    }

    @Test
    void blockerHitForBlockerThreshold() {
        ReviewResult x = r(Map.of("BLOCKER", 1), 1);
        assertThat(x.reachedFailOn(FailOn.BLOCKER)).isTrue();
    }

    @Test
    void criticalHitsBlockerThresholdToo() {
        ReviewResult x = r(Map.of("CRITICAL", 1), 1);
        assertThat(x.reachedFailOn(FailOn.BLOCKER)).isFalse();
        assertThat(x.reachedFailOn(FailOn.CRITICAL)).isTrue();
    }

    @Test
    void infoThresholdAny() {
        ReviewResult x = r(Map.of("INFO", 1), 1);
        assertThat(x.reachedFailOn(FailOn.INFO)).isTrue();
    }

    @Test
    void nullSeverityMapSafe() {
        ReviewResult x = new ReviewResult(
                "r", "t", Path.of("j"), Path.of("m"), Path.of("h"),
                0, null, 0, 0, Map.of(), 1.0, true, FailOn.CRITICAL);
        assertThat(x.reachedFailOn(FailOn.CRITICAL)).isFalse();
    }
}