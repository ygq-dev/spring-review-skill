package dev.springreview.engine;

import java.util.Locale;
import java.util.Set;

/**
 * SKILL.md §8「边界与豁免」的实现：
 * - 生成代码 / 第三方代码：全部跳过（§8 明确默认跳过）。
 * - 测试代码：STYLE、PERF 等非安全类规则豁免；SEC 类降级为 MEDIUM 并提示人工确认。
 * 路径清单与 SKILL.md §8 保持一致；扩展类别只需改这里的两个 Set。
 */
public final class BoundaryPolicy {

    /** 测试路径下直接豁免的规则类别（§8：STYLE、PERF 等非安全类）。 */
    public static final Set<String> TEST_EXEMPT_CATEGORIES = Set.of("STYLE", "PERF");

    /** 测试路径下降级置信度的规则类别（§8：安全类不自动豁免，但测试夹具假凭据常见）。 */
    public static final Set<String> TEST_DOWNGRADE_CATEGORIES = Set.of("SEC");

    private BoundaryPolicy() {
    }

    public enum Decision { KEEP, DROP_EXEMPT, SKIP_GENERATED_OR_VENDOR, DOWNGRADE }

    public static boolean isTestPath(String file) {
        if (file == null || file.isBlank()) {
            return false;
        }
        String p = normalize(file);
        return p.contains("/src/test/")
            || p.contains("/src/it/")
            || p.startsWith("test/")
            || p.contains("/test/java/")
            || endsWithAny(p, "/Test.java", "Test.java", "Tests.java", "IT.java", "TestCase.java");
    }

    public static boolean isGeneratedOrVendorPath(String file) {
        if (file == null || file.isBlank()) {
            return false;
        }
        String p = normalize(file);
        String name = p.substring(p.lastIndexOf('/') + 1);
        // 注意：路径可能以 vendor/ 等开头（无前导斜杠），startsWith 与 contains 必须成对出现
        return p.startsWith("vendor/") || p.startsWith("third_party/")
            || p.contains("/vendor/") || p.contains("/third_party/")
            || p.contains("/generated-sources/")
            || p.contains("/generated/")
            || name.endsWith("Generated.java")
            || name.endsWith(".g.java");
    }

    /**
     * 判定一条候选问题的处理方式。
     *
     * @param file          命中文件路径
     * @param categoryL1    规则一级分类（如 SEC、STYLE）
     * @param confidence    原置信度
     * @return KEEP 原样保留；DOWNGRADE 降置信度并附提示；其余为跳过
     */
    public static Decision decide(String file, String categoryL1, String confidence) {
        if (isGeneratedOrVendorPath(file)) {
            return Decision.SKIP_GENERATED_OR_VENDOR;
        }
        if (!isTestPath(file)) {
            return Decision.KEEP;
        }
        String cat = categoryL1 == null ? "" : categoryL1.trim().toUpperCase(Locale.ROOT);
        if (TEST_EXEMPT_CATEGORIES.contains(cat)) {
            return Decision.DROP_EXEMPT;
        }
        if (TEST_DOWNGRADE_CATEGORIES.contains(cat) && !"MEDIUM".equalsIgnoreCase(confidence)) {
            return Decision.DOWNGRADE;
        }
        return Decision.KEEP;
    }

    /** SEC 测试路径命中的附加说明，追加到 message。 */
    public static String downgradeNote() {
        return "（测试路径：假凭据/夹具常见，建议人工确认）";
    }

    private static String normalize(String file) {
        return file.replace('\\', '/').replace("//", "/");
    }

    private static boolean endsWithAny(String path, String... suffixes) {
        for (String s : suffixes) {
            if (path.endsWith(s)) {
                return true;
            }
        }
        return false;
    }
}
