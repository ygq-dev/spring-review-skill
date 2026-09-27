package dev.springreview.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MessageTemplates：插值、缺变量剥离、残留清理。
 */
class MessageTemplatesTest {

    @Test
    void replacesKnownPlaceholders() {
        String out = MessageTemplates.render(
            "Controller {className} 直接依赖 {repositoryName}，违反分层依赖。",
            Map.of("className", "UserController", "repositoryName", "UserRepository"));
        assertEquals("Controller UserController 直接依赖 UserRepository，违反分层依赖。", out);
    }

    @Test
    void stripsUnknownPlaceholdersInsteadOfLeaking() {
        String out = MessageTemplates.render(
            "生产构建声明了 SNAPSHOT 依赖 {dependency}。",
            Map.of());
        assertFalse(out.contains("{"));
        assertFalse(out.contains("}"));
        assertEquals("生产构建声明了 SNAPSHOT 依赖。", out);
    }

    @Test
    void nullVarsTreatedAsAbsent() {
        String out = MessageTemplates.render("@Value 注入 {field} 缺少默认值。", null);
        assertFalse(out.contains("{field}"));
    }

    @Test
    void blankTemplatePassedThrough() {
        assertEquals("", MessageTemplates.render("", Map.of("a", "b")));
    }

    @Test
    void valueIsTrimmed() {
        String out = MessageTemplates.render("检测到 {key}", Map.of("key", "  debug=true  "));
        assertEquals("检测到 debug=true", out);
    }

    @Test
    void nonLetterTokensNotTreatedAsPlaceholders() {
        // JSON 片段或集合字面量 {0}、{@link} 之外的数字占位不受影响
        String out = MessageTemplates.render("found {0} items", Map.of());
        assertTrue(out.contains("{0}"));
    }
}
