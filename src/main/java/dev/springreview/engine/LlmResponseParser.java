package dev.springreview.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springreview.observability.Logs;
import dev.springreview.rules.Rule;
import dev.springreview.tools.IssueCandidate;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 解析 LLM 的 JSON 响应为 IssueCandidate 列表。
 * 非法/缺字段的条目跳过。
 */
public final class LlmResponseParser {

    private static final Logger LOG = Logs.logger("llm-parser");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> VALID_CONF = Set.of("HIGH", "MEDIUM", "LOW");

    public List<IssueCandidate> parse(String content, Rule rule, String traceId) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(content);
        } catch (IOException ex) {
            LOG.warn("llm response not json rule={} trace={}: {}",
                rule.id(), traceId, ex.getMessage());
            return List.of();
        }
        JsonNode arr = root.path("issues");
        if (!arr.isArray() || arr.isEmpty()) {
            return List.of();
        }
        List<IssueCandidate> out = new ArrayList<>();
        for (JsonNode n : arr) {
            String file = n.path("file").asText("");
            int line = n.path("line").asInt(0);
            int col = n.path("column").asInt(0);
            String evidence = n.path("evidence").asText("");
            String message = n.path("message").asText("");
            String confidence = n.path("confidence").asText(rule.confidence());
            if (file.isEmpty() || line <= 0 || message.isEmpty()) {
                continue;
            }
            if (!VALID_CONF.contains(confidence)) {
                confidence = "LOW";
            }
            out.add(new IssueCandidate(
                rule.id(),
                rule.severity(),
                confidence,
                "HYBRID",
                file,
                line,
                Math.max(1, col),
                evidence,
                message,
                rule.remediation(),
                "CUSTOM",
                rule.toolRuleId(),
                "llm:" + traceId));
        }
        return out;
    }
}
