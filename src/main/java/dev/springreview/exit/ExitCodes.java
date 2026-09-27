package dev.springreview.exit;

/**
 * 退出码定义，严格对齐 E2/E11 冻结。
 */
public final class ExitCodes {

    public static final int SUCCESS = 0;
    public static final int FAIL_ON_HIT = 1;
    public static final int CLI_CONFIG = 2;
    public static final int SCOPE = 3;
    public static final int RULE_LOAD = 4;
    public static final int REPORT = 5;
    public static final int INTERNAL = 6;

    private ExitCodes() {
    }
}