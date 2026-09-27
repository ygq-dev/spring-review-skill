package dev.springreview.engine;

import dev.springreview.llm.Message;
import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * 构造 LLM 请求消息。
 * - system 放规则说明与输出 schema
 * - user 放脱敏后的代码片段
 * - 总长限制，避免超大 prompt
 */
public final class LlmPromptBuilder {

    private static final int MAX_USER_BYTES = 24_000;
    private static final int MAX_PER_FILE_BYTES = 6_000;

    public List<Message> build(LlmSemanticRequest req) {
        Rule r = req.rule();
        StringBuilder sys = new StringBuilder(512);
        sys.append("你是一个 Spring 代码审查助手。仅根据给定规则判断，不得编造。\n\n");
        sys.append("规则 ID：").append(r.id()).append('\n');
        sys.append("规则名称：").append(r.name()).append('\n');
        sys.append("规则描述：").append(r.description()).append('\n');
        sys.append("严重级别：").append(r.severity()).append('\n');
        sys.append("修复建议：").append(r.remediation()).append('\n');
        sys.append("\n如果代码违反规则，返回如下 JSON；无违反返回 {\"issues\":[]}。\n");
        sys.append("{\"issues\":[{\"file\":\"相对仓库路径\",\"line\":1,\"column\":1,");
        sys.append("\"evidence\":\"证据片段\",\"message\":\"问题描述\",");
        sys.append("\"confidence\":\"HIGH|MEDIUM|LOW\"}]}\n");
        sys.append("严格输出单个 JSON 对象，不要输出多余文字。");

        StringBuilder user = new StringBuilder(4096);
        user.append("--- 代码 ---\n");
        int totalBytes = 0;
        for (SourceUnit u : req.units()) {
            String path = u.path();
            String content = u.content();
            if (content == null) {
                continue;
            }
            String truncated = truncateUtf8(content, MAX_PER_FILE_BYTES);
            String block = "\n// FILE: " + path + "\n" + truncated + "\n";
            int blockBytes = block.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (totalBytes + blockBytes > MAX_USER_BYTES) {
                break;
            }
            user.append(block);
            totalBytes += blockBytes;
        }

        List<Message> out = new ArrayList<>(2);
        out.add(Message.system(sys.toString()));
        out.add(Message.user(user.toString()));
        return out;
    }

    private static String truncateUtf8(String s, int maxBytes) {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return s;
        }
        return new String(bytes, 0, maxBytes, java.nio.charset.StandardCharsets.UTF_8)
            + "\n// ...（已截断）";
    }
}
