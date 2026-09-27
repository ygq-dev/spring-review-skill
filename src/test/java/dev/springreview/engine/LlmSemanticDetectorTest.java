package dev.springreview.engine;

import dev.springreview.llm.LlmClient;
import dev.springreview.llm.LlmHealth;
import dev.springreview.llm.LlmRequest;
import dev.springreview.llm.LlmResponse;
import dev.springreview.parser.DependencyGraph;
import dev.springreview.parser.ParsedBundle;
import dev.springreview.rules.Rule;
import dev.springreview.scope.ReviewScope;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import dev.springreview.tools.IssueCandidate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LlmSemanticDetectorTest {

    private static Rule rule(boolean useLlm) {
        Map<String, Object> params = useLlm ? Map.of("use_llm", "true") : Map.of();
        return new Rule(
            "SRS-DAO-04-001", "事务内远程调用", "desc",
            "DAO", "04", List.of(), "ACTIVE", "CRITICAL", "MEDIUM",
            "HYBRID", "CUSTOM", null, params, true,
            new Rule.AppliesTo("17+", "3.x", List.of("maven"),
                List.of("all"), List.of(), List.of()),
            "message", "remediation", List.of(),
            "1.0.0", "1.0.0", "1.0.0", "team", List.of());
    }

    private static LlmClient clientReturning(String json) {
        return new LlmClient() {
            @Override
            public LlmResponse complete(LlmRequest request) {
                return LlmResponse.success(json,
                    new LlmResponse.Usage(1, 1, 2), "stop", 10);
            }

            @Override
            public LlmHealth health() {
                return new LlmHealth("UP", "test", "m", 0, null, Instant.now(), "");
            }

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public String provider() {
                return "test";
            }
        };
    }

    private static SourceBundle bundle() {
        SourceUnit u = new SourceUnit("src/A.java", "java",
            "class A {}", "sha256:x", 1, "_root", List.of());
        return new SourceBundle("/r", ReviewScope.Mode.FILES,
            List.of(u), null,
            new SourceBundle.Stats(1, 1, 0, 0, 0));
    }

    private static ParsedBundle emptyParsed() {
        return new ParsedBundle("b", ParsedBundle.Mode.FULL,
            List.of(), DependencyGraph.empty(), List.of(),
            new ParsedBundle.Stats(0, 0, 0, 0, 0, 0, 0), false);
    }

    @Test
    void supportsOnlyWhenUseLlmTrue() {
        LlmSemanticDetector d = new LlmSemanticDetector(clientReturning("{}"));
        assertThat(d.supports(rule(true))).isTrue();
        assertThat(d.supports(rule(false))).isFalse();
    }

    @Test
    void skipWhenClientDisabled() {
        LlmClient disabled = new LlmClient() {
            @Override public LlmResponse complete(LlmRequest r) { return null; }
            @Override public LlmHealth health() {
                return new LlmHealth("OFFLINE", "", "", 0, null, Instant.now(), "");
            }
            @Override public boolean enabled() { return false; }
            @Override public String provider() { return ""; }
        };
        LlmSemanticDetector d = new LlmSemanticDetector(disabled);
        assertThat(d.supports(rule(true))).isFalse();
    }

    @Test
    void parsesValidIssues() {
        String json = "{\"issues\":[{\"file\":\"src/A.java\",\"line\":5,"
            + "\"column\":1,\"evidence\":\"RestTemplate.invoke()\","
            + "\"message\":\"事务内远程调用\",\"confidence\":\"HIGH\"}]}";
        LlmSemanticDetector d = new LlmSemanticDetector(clientReturning(json));
        List<IssueCandidate> out = d.detect(rule(true), bundle(), emptyParsed());
        assertThat(out).hasSize(1);
        assertThat(out.get(0).ruleId()).isEqualTo("SRS-DAO-04-001");
        assertThat(out.get(0).line()).isEqualTo(5);
        assertThat(out.get(0).confidence()).isEqualTo("HIGH");
    }

    @Test
    void skipsInvalidEntries() {
        String json = "{\"issues\":[{\"file\":\"\",\"line\":5,\"message\":\"x\"},"
            + "{\"file\":\"A.java\",\"line\":0,\"message\":\"x\"},"
            + "{\"file\":\"A.java\",\"line\":1,\"message\":\"\"},"
            + "{\"file\":\"A.java\",\"line\":1,\"message\":\"ok\"}]}";
        LlmSemanticDetector d = new LlmSemanticDetector(clientReturning(json));
        List<IssueCandidate> out = d.detect(rule(true), bundle(), emptyParsed());
        assertThat(out).hasSize(1);
    }

    @Test
    void malformedResponseYieldsEmpty() {
        LlmSemanticDetector d = new LlmSemanticDetector(clientReturning("not json"));
        List<IssueCandidate> out = d.detect(rule(true), bundle(), emptyParsed());
        assertThat(out).isEmpty();
    }

    @Test
    void invalidConfidenceDowngraded() {
        String json = "{\"issues\":[{\"file\":\"A.java\",\"line\":1,"
            + "\"column\":1,\"message\":\"x\",\"confidence\":\"SUPER\"}]}";
        LlmSemanticDetector d = new LlmSemanticDetector(clientReturning(json));
        List<IssueCandidate> out = d.detect(rule(true), bundle(), emptyParsed());
        assertThat(out).hasSize(1);
        assertThat(out.get(0).confidence()).isEqualTo("LOW");
    }
}
