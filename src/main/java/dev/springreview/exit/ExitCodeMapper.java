package dev.springreview.exit;

import dev.springreview.config.AppConfig;
import dev.springreview.orchestration.ReviewResult;

/**
 * 将 ReviewResult 或异常映射为退出码。
 * 0 成功；1 达到 failOn；2 CLI/配置；3 范围；4 规则加载；5 报告/输出/校验；6 内部错误。
 */
public final class ExitCodeMapper {

    private ExitCodeMapper() {
    }

    public static int map(ReviewResult result, AppConfig config) {
        if (result == null) {
            return ExitCodes.INTERNAL;
        }
        if (!result.schemaValid()) {
            return ExitCodes.REPORT;
        }
        return result.reachedFailOn(config.exit().failOn())
                ? ExitCodes.FAIL_ON_HIT
                : ExitCodes.SUCCESS;
    }

    public static int mapException(Throwable t) {
        if (t instanceof SpringReviewException sre) {
            return sre.exitCode();
        }
        return ExitCodes.INTERNAL;
    }
}