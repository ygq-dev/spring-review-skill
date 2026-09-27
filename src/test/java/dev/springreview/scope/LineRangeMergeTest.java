package dev.springreview.scope;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通过反射调 GitDiffCollector 的私有 mergeAdjacent 不优雅；
 * 这里直接测试 LineRange 的 overlaps/merge 语义，间接覆盖合并逻辑。
 */
class LineRangeMergeTest {

    @Test
    void overlapsTrueWhenTouching() {
        LineRange a = new LineRange(1, 5, LineRange.Side.NEW, LineRange.ChangeType.MODIFY);
        LineRange b = new LineRange(5, 10, LineRange.Side.NEW, LineRange.ChangeType.MODIFY);
        assertThat(a.overlaps(b)).isTrue();
    }

    @Test
    void overlapsFalseWhenSeparated() {
        LineRange a = new LineRange(1, 5, LineRange.Side.NEW, LineRange.ChangeType.MODIFY);
        LineRange b = new LineRange(7, 10, LineRange.Side.NEW, LineRange.ChangeType.MODIFY);
        assertThat(a.overlaps(b)).isFalse();
    }

    @Test
    void mergeProducesUnionRange() {
        LineRange a = new LineRange(1, 5, LineRange.Side.NEW, LineRange.ChangeType.MODIFY);
        LineRange b = new LineRange(4, 10, LineRange.Side.NEW, LineRange.ChangeType.MODIFY);
        LineRange m = a.merge(b);
        assertThat(m.start()).isEqualTo(1);
        assertThat(m.end()).isEqualTo(10);
    }

    @Test
    void invalidRangeRejected() {
        try {
            new LineRange(0, 5, LineRange.Side.NEW, LineRange.ChangeType.ADD);
            throw new AssertionError("expected exception");
        } catch (IllegalArgumentException expected) {
            // ok
        }
        try {
            new LineRange(6, 5, LineRange.Side.NEW, LineRange.ChangeType.ADD);
            throw new AssertionError("expected exception");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    void lengthIsInclusive() {
        LineRange r = new LineRange(10, 20, LineRange.Side.NEW, LineRange.ChangeType.ADD);
        assertThat(r.length()).isEqualTo(11);
    }

    @SuppressWarnings("unused")
    private static List<LineRange> dummy() {
        return List.of();
    }
}
