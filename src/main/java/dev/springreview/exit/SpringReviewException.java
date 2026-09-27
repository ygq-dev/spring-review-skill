package dev.springreview.exit;

/**
 * 审查流程统一异常，携带退出码。
 * 用法：抛出时按失败阶段选择工厂方法，M19 直接读取 exitCode()。
 */
public class SpringReviewException extends RuntimeException {

    private final int exitCode;

    private SpringReviewException(int exitCode, String message, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }

    public static SpringReviewException cli(String message) {
        return new SpringReviewException(ExitCodes.CLI_CONFIG, message, null);
    }

    public static SpringReviewException cli(String message, Throwable cause) {
        return new SpringReviewException(ExitCodes.CLI_CONFIG, message, cause);
    }

    public static SpringReviewException scope(String message, Throwable cause) {
        return new SpringReviewException(ExitCodes.SCOPE, message, cause);
    }

    public static SpringReviewException ruleLoad(String message, Throwable cause) {
        return new SpringReviewException(ExitCodes.RULE_LOAD, message, cause);
    }

    public static SpringReviewException report(String message, Throwable cause) {
        return new SpringReviewException(ExitCodes.REPORT, message, cause);
    }

    public static SpringReviewException internal(String message, Throwable cause) {
        return new SpringReviewException(ExitCodes.INTERNAL, message, cause);
    }
}