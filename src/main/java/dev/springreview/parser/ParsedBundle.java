package dev.springreview.parser;

import java.util.List;
import java.util.Map;

/**
 * M09 批量解析输出。
 */
public record ParsedBundle(
    String bundleId,
    Mode mode,
    List<ParsedSource> parsedSources,
    DependencyGraph dependencyGraph,
    List<ParseError> parseErrors,
    Stats stats,
    boolean partial
) {

    public enum Mode {FULL, DIFF}

    public record Stats(
        int totalFiles, int parsedCount, int skippedCount,
        int edgeCount, int typeCount, int methodCount, long durationMs
    ) {
    }

    public ParsedSource findByPath(String path) {
        for (ParsedSource s : parsedSources) {
            if (s.path().equals(path)) {
                return s;
            }
        }
        return null;
    }

    public Map<String, ParsedSource> indexByPath() {
        Map<String, ParsedSource> out = new java.util.LinkedHashMap<>();
        for (ParsedSource s : parsedSources) {
            out.put(s.path(), s);
        }
        return out;
    }
}
