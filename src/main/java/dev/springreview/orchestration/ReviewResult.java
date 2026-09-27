package dev.springreview.orchestration;

import dev.springreview.config.FailOn;

import java.nio.file.Path;
import java.util.Map;

/**
 * 单次审查结果，供 M19 映射退出码与打印摘要。
 * F2 骨架字段：F3 起补充 ReviewReport 引用、真实 metrics。
 */
public record ReviewResult(
        String reportId,
        String traceId,
        Path latestJson,
        Path latestMarkdown,
        Path historyDir,
        int totalIssues,
        Map<String, Integer> bySeverity,
        int blocking,
        long durationMs,
        Map<String, Long> tokenUsage,
        double toolSuccessRate,
        boolean schemaValid,
        FailOn failOn
) {

    public boolean reachedFailOn(FailOn configured) {
        FailOn effective = configured == null ? FailOn.CRITICAL : configured;
        if (effective == FailOn.NEVER) {
            return false;
        }
        return switch (effective) {
            case BLOCKER -> count("BLOCKER") > 0;
            case CRITICAL -> count("BLOCKER") > 0 || count("CRITICAL") > 0;
            case MAJOR -> count("BLOCKER") > 0 || count("CRITICAL") > 0 || count("MAJOR") > 0;
            case MINOR -> count("BLOCKER") > 0 || count("CRITICAL") > 0
                    || count("MAJOR") > 0 || count("MINOR") > 0;
            case INFO -> totalIssues > 0;
            case NEVER -> false;
        };
    }

    private int count(String sev) {
        if (bySeverity == null) {
            return 0;
        }
        Integer v = bySeverity.get(sev);
        return v == null ? 0 : v;
    }
}