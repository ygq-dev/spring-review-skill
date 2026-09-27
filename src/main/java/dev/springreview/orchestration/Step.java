package dev.springreview.orchestration;

/**
 * SKILL 8 步工作流的步骤标识。
 */
public enum Step {
    PARSE_INPUT,
    LOAD_RULES,
    COLLECT_CODE,
    STATIC_ANALYSIS,
    RULE_MATCH,
    AGGREGATE,
    REPORT_JSON,
    REPORT_MARKDOWN
}