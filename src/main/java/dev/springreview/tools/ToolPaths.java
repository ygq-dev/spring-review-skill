package dev.springreview.tools;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 工具路径解析：skillHome + 环境变量。
 * skillHome = SPRING_REVIEW_HOME（若设置）否则 user.dir。
 * 目标仓库不参与工具定位。
 */
public final class ToolPaths {

    private ToolPaths() {
    }

    /** skill 包根：SPRING_REVIEW_HOME 或 user.dir。 */
    public static Path skillHome() {
        String env = System.getenv("SPRING_REVIEW_HOME");
        if (env != null && !env.isBlank()) {
            return Path.of(env).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }

    public static Path checkstyleJar() {
        Path env = envPath("CHECKSTYLE_JAR");
        if (env != null && Files.exists(env)) {
            return env;
        }
        Path byConvention = skillHome().resolve("tools/checkstyle-10.17.0-all.jar");
        return Files.exists(byConvention) ? byConvention : null;
    }

    public static Path checkstyleConfig() {
        Path p = skillHome().resolve("config/checkstyle/checkstyle.xml");
        return Files.exists(p) ? p : null;
    }

    public static Path pmdExecutable() {
        Path home = envPath("PMD_HOME");
        if (home != null) {
            Path exe = firstExisting(home.resolve("bin/pmd"), home.resolve("bin/pmd.bat"));
            if (exe != null) {
                return exe;
            }
        }
        Path root = skillHome().resolve("tools/pmd-bin-7.4.0");
        return firstExisting(root.resolve("bin/pmd"), root.resolve("bin/pmd.bat"));
    }

    public static Path pmdConfig() {
        Path p = skillHome().resolve("config/pmd/pmd.xml");
        return Files.exists(p) ? p : null;
    }

    public static Path spotbugsExecutable() {
        Path home = envPath("SPOTBUGS_HOME");
        if (home != null) {
            Path exe = firstExisting(home.resolve("bin/spotbugs"), home.resolve("bin/spotbugs.bat"));
            if (exe != null) {
                return exe;
            }
        }
        Path root = skillHome().resolve("tools/spotbugs-4.8.6");
        return firstExisting(root.resolve("bin/spotbugs"), root.resolve("bin/spotbugs.bat"));
    }

    private static Path firstExisting(Path... candidates) {
        for (Path p : candidates) {
            if (p != null && Files.exists(p)) {
                return p;
            }
        }
        return null;
    }

    private static Path envPath(String name) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? null : Path.of(v);
    }
}
