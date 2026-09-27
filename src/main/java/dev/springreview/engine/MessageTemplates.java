package dev.springreview.engine;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则 message 模板插值。
 * 规则 YAML 的 message 可含 {token} 占位符；检测器提供同名变量后替换，
 * 无法提供变量的占位符整体移除——报告层绝不允许出现未插值的原始 token。
 */
public final class MessageTemplates {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)}");

    private MessageTemplates() {
    }

    /**
     * 用 vars 中的值替换 template 里的 {key}。
     * vars 缺失的占位符替换为空串，随后清理多余空白与孤立标点。
     */
    public static String render(String template, Map<String, String> vars) {
        if (template == null || template.isBlank()) {
            return template;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder(template.length());
        while (m.find()) {
            String key = m.group(1);
            String val = vars == null ? null : vars.get(key);
            String rep = (val == null || val.isBlank()) ? "" : val.trim();
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return tidy(sb.toString());
    }

    /** 清理插值后的残留空白与悬空标点。 */
    private static String tidy(String s) {
        String out = s.replaceAll("\\s{2,}", " ");
        out = out.replaceAll("\\s+([，。：；、,.!?:：])", "$1");
        return out.trim();
    }
}
