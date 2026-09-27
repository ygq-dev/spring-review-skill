package dev.springreview.parser;

import java.util.List;
import java.util.Map;

/**
 * 包依赖图 + 类依赖图。
 * M09 只提供原始图；层级映射由 M12 按规则元数据完成。
 */
public record DependencyGraph(
    Map<String, List<DependencyEdge>> packageEdges,
    Map<String, List<DependencyEdge>> classEdges
) {

    public static DependencyGraph empty() {
        return new DependencyGraph(Map.of(), Map.of());
    }

    public boolean isEmpty() {
        return packageEdges.isEmpty() && classEdges.isEmpty();
    }
}
