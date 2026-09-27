package dev.springreview;

import dev.springreview.cli.ReviewCommand;
import picocli.CommandLine;

/**
 * Spring Review Skill 入口。
 * 只读审查；不修改目标仓库；不提交；不训练模型。
 */
public final class SpringReviewMain {

    private SpringReviewMain() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ReviewCommand())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        System.exit(exitCode);
    }
}