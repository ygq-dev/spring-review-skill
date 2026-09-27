package dev.springreview.engine;

import dev.springreview.llm.LlmClient;
import dev.springreview.llm.LlmRequest;
import dev.springreview.llm.LlmResponse;
import dev.springreview.observability.Logs;
import dev.springreview.parser.ParsedBundle;
import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import dev.springreview.tools.IssueCandidate;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * LLM 语义规则检测器。
 * 触发条件（全部满足）：
 * 1. LlmClient 存在且 enabled=true
 * 2. rule.detection_method ∈ {HYBRID, HEURISTIC, RULE_ENGINE}
 * 3. rule.parameters.use_llm = true（显式声明）
 * 无 use_llm 声明的规则不会走 LLM。
 */
public final class LlmSemanticDetector implements Detector {

    private static final Logger LOG = Logs.logger("llm-detector");
    private static final Set<String> ELIGIBLE = Set.of("HYBRID", "HEURISTIC", "RULE_ENGINE");

    private final LlmClient client;
    private final LlmPromptBuilder promptBuilder = new LlmPromptBuilder();
    private final LlmResponseParser responseParser = new LlmResponseParser();

    public LlmSemanticDetector(LlmClient client) {
        this.client = client;
    }

    @Override
    public boolean supports(Rule rule) {
        if (client == null || !client.enabled()) {
            return false;
        }
        if (rule.detectionMethod() == null
            || !ELIGIBLE.contains(rule.detectionMethod().toUpperCase())) {
            return false;
        }
        if (rule.parameters() == null) {
            return false;
        }
        Object v = rule.parameters().get("use_llm");
        return v != null && "true".equalsIgnoreCase(String.valueOf(v));
    }

    @Override
    public List<IssueCandidate> detect(Rule rule, SourceBundle bundle, ParsedBundle parsed) {
        List<SourceUnit> units = bundle.units();
        if (units.isEmpty()) {
            return List.of();
        }
        LlmSemanticRequest req = new LlmSemanticRequest(rule, units, currentTrace());
        LlmRequest llmReq = new LlmRequest(
            null, null,
            promptBuilder.build(req),
            null, null,
            LlmRequest.ResponseFormat.JSON_OBJECT,
            null, currentTrace());

        LlmResponse resp;
        try {
            resp = client.complete(llmReq);
        } catch (RuntimeException ex) {
            LOG.warn("llm call failed rule={} msg={}", rule.id(), ex.getMessage());
            return List.of();
        }
        if (!resp.isSuccess()) {
            LOG.warn("llm error rule={} code={} msg={}",
                rule.id(),
                resp.error() == null ? "?" : resp.error().code(),
                resp.error() == null ? "?" : resp.error().message());
            return List.of();
        }
        List<IssueCandidate> out = responseParser.parse(resp.content(), rule, currentTrace());
        if (!out.isEmpty()) {
            LOG.info("llm semantic hit rule={} count={}", rule.id(), out.size());
        }
        return out;
    }

    private static String currentTrace() {
        String t = dev.springreview.observability.Trace.current();
        return t == null ? "" : t;
    }
}
