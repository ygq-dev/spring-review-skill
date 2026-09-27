package dev.springreview.engine;

import dev.springreview.rules.Rule;
import dev.springreview.rules.RuleSet;
import dev.springreview.tools.IssueCandidate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IssueAggregatorTest {

    private static RuleSet rules() {
        Rule r = new Rule(
            "SRS-SEC-01-001", "硬编码密钥", "", "SEC", "01",
            List.of(), "ACTIVE", "BLOCKER", "HIGH", "REGEX", "CUSTOM",
            null, Map.of(), true,
            new Rule.AppliesTo("17+", "3.x", List.of("maven"), List.of("all"),
                List.of(), List.of()),
            "m", "r", List.of(), "1.0.0", "1.0.0", "1.0.0",
            "team", List.of());
        return new RuleSet(List.of(r), "1.0.0", Instant.now(),
            "rules", List.of(), Map.of(r.id(), r));
    }

    @Test
    void dedupesSameRuleFileLine() {
        IssueCandidate c1 = new IssueCandidate(
            "SRS-SEC-01-001", "BLOCKER", "MEDIUM", "REGEX",
            "A.java", 5, 1, "evi", "msg", "rem", "CUSTOM", null, null);
        IssueCandidate c2 = new IssueCandidate(
            "SRS-SEC-01-001", "BLOCKER", "HIGH", "REGEX",
            "A.java", 5, 1, "evi", "msg", "rem", "CUSTOM", null, null);

        AggregatedResult r = new IssueAggregator().aggregate(
            List.of(c1, c2), rules(), List.of(), 100, Map.of());
        assertThat(r.issues()).hasSize(1);
        assertThat(r.issues().get(0).confidence()).isEqualTo("HIGH");
    }

    @Test
    void sortsBySeverityThenConfidence() {
        IssueCandidate blocker = new IssueCandidate(
            "SRS-SEC-01-001", "BLOCKER", "HIGH", "REGEX",
            "A.java", 1, 1, "e", "m", "r", "CUSTOM", null, null);
        IssueCandidate major = new IssueCandidate(
            "SRS-SEC-01-001", "MAJOR", "HIGH", "REGEX",
            "A.java", 2, 1, "e", "m", "r", "CUSTOM", null, null);
        IssueCandidate minor = new IssueCandidate(
            "SRS-SEC-01-001", "MINOR", "HIGH", "REGEX",
            "A.java", 3, 1, "e", "m", "r", "CUSTOM", null, null);

        AggregatedResult r = new IssueAggregator().aggregate(
            List.of(minor, major, blocker), rules(), List.of(), 100, Map.of());
        assertThat(r.issues()).extracting(Issue::severity)
            .containsExactly("BLOCKER", "MAJOR", "MINOR");
    }

    @Test
    void blockingCountsBlockerAndCritical() {
        IssueCandidate b = new IssueCandidate(
            "SRS-SEC-01-001", "BLOCKER", "HIGH", "REGEX",
            "A.java", 1, 1, "e", "m", "r", "CUSTOM", null, null);
        IssueCandidate c = new IssueCandidate(
            "SRS-SEC-01-001", "CRITICAL", "HIGH", "REGEX",
            "A.java", 2, 1, "e", "m", "r", "CUSTOM", null, null);
        IssueCandidate m = new IssueCandidate(
            "SRS-SEC-01-001", "MINOR", "HIGH", "REGEX",
            "A.java", 3, 1, "e", "m", "r", "CUSTOM", null, null);

        AggregatedResult r = new IssueAggregator().aggregate(
            List.of(b, c, m), rules(), List.of(), 100, Map.of());
        assertThat(r.summary().blocking()).isEqualTo(2);
        assertThat(r.summary().totalIssues()).isEqualTo(3);
    }

    @Test
    void issueIdFormat() {
        IssueCandidate c = new IssueCandidate(
            "SRS-SEC-01-001", "BLOCKER", "HIGH", "REGEX",
            "A.java", 1, 1, "e", "m", "r", "CUSTOM", null, null);
        AggregatedResult r = new IssueAggregator().aggregate(
            List.of(c), rules(), List.of(), 100, Map.of());
        assertThat(r.issues().get(0).issueId()).matches("ISSUE-[0-9A-F]{8}");
    }
}
