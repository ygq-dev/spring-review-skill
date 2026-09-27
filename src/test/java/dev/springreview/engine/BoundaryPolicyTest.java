package dev.springreview.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BoundaryPolicy：SKILL.md §8 边界与豁免的路径判定。
 */
class BoundaryPolicyTest {

    @Test
    void testPathsRecognized() {
        assertTrue(BoundaryPolicy.isTestPath("src/test/java/dev/app/FooTest.java"));
        assertTrue(BoundaryPolicy.isTestPath("app/src/test/java/Foo.java"));
        assertTrue(BoundaryPolicy.isTestPath("a/b/FooTest.java"));
        assertTrue(BoundaryPolicy.isTestPath("a/b/FooTests.java"));
        assertTrue(BoundaryPolicy.isTestPath("a/b/FooIT.java"));
        assertTrue(BoundaryPolicy.isTestPath("a/b/FooTestCase.java"));
        assertFalse(BoundaryPolicy.isTestPath("src/main/java/dev/app/Foo.java"));
        assertFalse(BoundaryPolicy.isTestPath("src/main/java/dev/app/Contest.java"));
    }

    @Test
    void generatedAndVendorAlwaysSkipped() {
        assertEquals(BoundaryPolicy.Decision.SKIP_GENERATED_OR_VENDOR,
            BoundaryPolicy.decide("target/generated-sources/x/Foo.java", "SEC", "HIGH"));
        assertEquals(BoundaryPolicy.Decision.SKIP_GENERATED_OR_VENDOR,
            BoundaryPolicy.decide("vendor/lib/Foo.java", "DAO", "HIGH"));
        assertEquals(BoundaryPolicy.Decision.SKIP_GENERATED_OR_VENDOR,
            BoundaryPolicy.decide("a/b/FooGenerated.java", "DAO", "HIGH"));
        // §8 glob 是 **/*Generated.java（以 Generated.java 结尾），GeneratedFoo.java 不属于生成代码
        assertEquals(BoundaryPolicy.Decision.KEEP,
            BoundaryPolicy.decide("a/b/GeneratedFoo.java", "DAO", "HIGH"));
    }

    @Test
    void testPathDropsStyleAndPerf() {
        assertEquals(BoundaryPolicy.Decision.DROP_EXEMPT,
            BoundaryPolicy.decide("src/test/java/FooTest.java", "STYLE", "HIGH"));
        assertEquals(BoundaryPolicy.Decision.DROP_EXEMPT,
            BoundaryPolicy.decide("src/test/java/FooTest.java", "PERF", "HIGH"));
    }

    @Test
    void testPathDowngradesSecurity() {
        assertEquals(BoundaryPolicy.Decision.DOWNGRADE,
            BoundaryPolicy.decide("src/test/java/FooTest.java", "SEC", "HIGH"));
        // 已是 MEDIUM 不重复降级
        assertEquals(BoundaryPolicy.Decision.KEEP,
            BoundaryPolicy.decide("src/test/java/FooTest.java", "SEC", "MEDIUM"));
    }

    @Test
    void testPathKeepsOtherCategories() {
        assertEquals(BoundaryPolicy.Decision.KEEP,
            BoundaryPolicy.decide("src/test/java/FooTest.java", "DAO", "HIGH"));
        assertEquals(BoundaryPolicy.Decision.KEEP,
            BoundaryPolicy.decide("src/test/java/FooTest.java", "EXC", "HIGH"));
    }

    @Test
    void mainPathAlwaysKeep() {
        assertEquals(BoundaryPolicy.Decision.KEEP,
            BoundaryPolicy.decide("src/main/java/Foo.java", "SEC", "HIGH"));
        assertEquals(BoundaryPolicy.Decision.KEEP,
            BoundaryPolicy.decide("src/main/java/Foo.java", "STYLE", "HIGH"));
    }

    @Test
    void downgradeNoteIsInformative() {
        assertFalse(BoundaryPolicy.downgradeNote().isBlank());
        assertTrue(BoundaryPolicy.downgradeNote().contains("人工确认"));
    }
}
