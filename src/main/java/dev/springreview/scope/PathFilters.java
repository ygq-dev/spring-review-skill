package dev.springreview.scope;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认 include / exclude 与用户 include / exclude 合并。
 * 默认排除不可被 include 覆盖。
 */
public final class PathFilters {

    /** 默认排除目录 */
    private static final List<String> DEFAULT_EXCLUDED_DIRS = List.of(
            "**/target/**", "**/build/**", "**/generated/**", "**/generated-sources/**",
            "**/vendor/**", "**/node_modules/**", "**/.git/**"
    );

    /** 默认排除二进制/图片/压缩包扩展名 */
    private static final List<String> DEFAULT_EXCLUDED_EXT = List.of(
            "class", "jar", "war", "ear", "so", "dll", "exe", "bin",
            "png", "jpg", "jpeg", "gif", "webp", "ico",
            "zip", "tar", "gz", "7z", "rar"
    );

    private final List<PathMatcher> includes;
    private final List<PathMatcher> excludes;
    private final boolean caseInsensitive;

    public PathFilters(List<String> userInclude, List<String> userExclude) {
        this.caseInsensitive = System.getProperty("os.name", "").toLowerCase().contains("win");
        this.includes = compile(userInclude == null || userInclude.isEmpty()
                ? List.of("**/*.java")
                : userInclude);
        List<String> combined = new ArrayList<>(DEFAULT_EXCLUDED_DIRS);
        if (userExclude != null) {
            combined.addAll(userExclude);
        }
        this.excludes = compile(combined);
    }

    private List<PathMatcher> compile(List<String> globs) {
        List<PathMatcher> out = new ArrayList<>(globs.size());
        for (String g : globs) {
            out.add(FileSystems.getDefault().getPathMatcher("glob:" + g));
        }
        return out;
    }

    public boolean includes(Path relative) {
        String name = relative.getFileName() != null ? relative.getFileName().toString() : "";
        if (isExcludedExtension(name)) {
            return false;
        }
        Path normalized = normalize(relative);
        for (PathMatcher m : excludes) {
            if (m.matches(normalized)) {
                return false;
            }
        }
        for (PathMatcher m : includes) {
            if (m.matches(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExcludedExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String ext = fileName.substring(dot + 1).toLowerCase();
        return DEFAULT_EXCLUDED_EXT.contains(ext);
    }

    private Path normalize(Path relative) {
        Path p = relative.normalize();
        if (caseInsensitive) {
            return Path.of(p.toString().toLowerCase());
        }
        return p;
    }
}