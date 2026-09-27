package dev.springreview.engine;

import dev.springreview.llm.LlmClient;
import dev.springreview.observability.Logs;
import dev.springreview.parser.ParsedBundle;
import dev.springreview.rules.Rule;
import dev.springreview.rules.SelectedRules;
import dev.springreview.scope.SourceBundle;
import dev.springreview.tools.IssueCandidate;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RuleEngine {

    private static final Logger LOG = Logs.logger("rule-engine");

    private final List<Detector> detectors;

    public RuleEngine() {
        this(null);
    }

    public RuleEngine(LlmClient llmClient) {
        List<Detector> list = new ArrayList<>();
        // 专用检测器优先（精准度高）
        list.add(new ArHybridDetector());
        list.add(new BuildFileDetector());
        list.add(new AstPreciseDetector());
        // 通用回退
        list.add(new ConfigKeyDetector());
        list.add(new RegexDetector());
        list.add(new AstHeuristicDetector());
        if (llmClient != null) {
            list.add(new LlmSemanticDetector(llmClient));
        }
        this.detectors = List.copyOf(list);
    }

    public MatchResult match(SelectedRules rules, SourceBundle bundle, ParsedBundle parsed) {
        Logs.module("M12");
        Instant start = Instant.now();
        List<IssueCandidate> all = new ArrayList<>();
        int matchedRuleCount = 0;
        for (Rule r : rules.rules()) {
            boolean matched = false;
            for (Detector d : detectors) {
                if (!d.supports(r)) {
                    continue;
                }
                matched = true;
                try {
                    List<IssueCandidate> c = d.detect(r, bundle, parsed);
                    if (!c.isEmpty()) {
                        all.addAll(c);
                    }
                } catch (RuntimeException ex) {
                    LOG.warn("detector failed rule={} detector={} msg={}",
                        r.id(), d.getClass().getSimpleName(), ex.getMessage());
                }
            }
            if (matched) {
                matchedRuleCount++;
            }
        }
        long dur = Duration.between(start, Instant.now()).toMillis();
        LOG.info("rule engine done candidates={} matchedRules={} durationMs={}",
            all.size(), matchedRuleCount, dur);
        return new MatchResult(List.copyOf(all), matchedRuleCount, dur);
    }

    public record MatchResult(
        List<IssueCandidate> candidates,
        int matchedRules,
        long durationMs
    ) {
    }
}
