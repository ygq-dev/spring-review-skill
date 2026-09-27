package dev.springreview.parser;

/**
 * 依赖图中的一条边。from 依赖 to。
 */
public record DependencyEdge(
    String from,
    String to,
    Kind kind,
    String sourceFile,
    int line,
    int column,
    String evidence
) {

    public enum Kind {
        IMPORT,
        STATIC_IMPORT,
        WILDCARD_IMPORT,
        EXTENDS,
        IMPLEMENTS,
        ANNOTATION,
        FIELD_TYPE,
        PARAM_TYPE,
        RETURN_TYPE,
        TYPE_REF
    }
}
