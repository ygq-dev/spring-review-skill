package dev.springreview.engine;

import dev.springreview.llm.LlmClient;
import dev.springreview.observability.Logs;
import dev.springreview.parser.ParsedBundle;
import dev.springreview.rules.Rule;
import dev.springreview.rules.SelectedRules;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import dev.springreview.tools.IssueCandidate;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
            SourceBundle ruleBundle = restrictToCategoryFiles(r, bundle);
            for (Detector d : detectors) {
                if (!d.supports(r)) {
                    continue;
                }
                matched = true;
                try {
                    List<IssueCandidate> c = d.detect(r, ruleBundle, parsed);
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

    /**
     * 类别→文件类型绑定：规则只在与其类别匹配的文件类型上运行。
     * BUILD 类规则只在 Maven/Gradle 构建文件上运行；CF 类规则只在
     * YAML/properties 配置文件上运行；其余类别维持全量文件。
     */
    private static SourceBundle restrictToCategoryFiles(Rule r, SourceBundle bundle) {
        if (bundle == null || bundle.units() == null || bundle.units().isEmpty()) {
            return bundle;
        }
        List<SourceUnit> filtered;
        switch (r.categoryL1() == null ? "" : r.categoryL1()) {
            case "BUILD" -> filtered = bundle.units().stream()
                .filter(u -> isBuildFile(u.path())).toList();
            case "CF" -> filtered = bundle.units().stream()
                .filter(u -> isConfigFile(u.path())).toList();
            default -> {
                return bundle;
            }
        }
        if (filtered.size() == bundle.units().size()) {
            return bundle;
        }
        return new SourceBundle(bundle.repoRoot(), bundle.mode(),
            filtered, bundle.diff(), bundle.stats());
    }

    private static boolean isBuildFile(String path) {
        if (path == null) {
            return false;
        }
        String p = path.replace('\\', '/');
        String name = p.substring(p.lastIndexOf('/') + 1);
        return name.equals("pom.xml")
            || name.equals("build.gradle") || name.equals("build.gradle.kts")
            || name.equals("settings.gradle") || name.equals("settings.gradle.kts")
            || name.equals("gradle.lockfile");
    }

    private static boolean isConfigFile(String path) {
        if (path == null) {
            return false;
        }
        String p = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        return p.endsWith(".yml") || p.endsWith(".yaml") || p.endsWith(".properties");
    }

    public record MatchResult(
        List<IssueCandidate> candidates,
        int matchedRules,
        long durationMs
    ) {
    }
}
