package dev.springreview.scope;

/**
 * 1-based 闭区间行范围。
 */
public record LineRange(int start, int end, Side side, ChangeType changeType) {

    public enum Side {OLD, NEW}

    public enum ChangeType {ADD, MODIFY, DELETE}

    public LineRange {
        if (start < 1 || end < start) {
            throw new IllegalArgumentException("invalid LineRange: [" + start + "," + end + "]");
        }
    }

    public int length() {
        return end - start + 1;
    }

    public boolean overlaps(LineRange other) {
        return this.end >= other.start && other.end >= this.start;
    }

    public LineRange merge(LineRange other) {
        return new LineRange(Math.min(start, other.start), Math.max(end, other.end),
                side, changeType);
    }
}