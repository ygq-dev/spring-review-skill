package dev.springreview.tools;

import dev.springreview.config.AppConfig;
import dev.springreview.observability.Logs;
import dev.springreview.rules.SelectedRules;
import dev.springreview.scope.SourceBundle;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StaticAnalysisFacade {

    private static final Logger LOG = Logs.logger("static-analysis");
    private static final long TOTAL_BUDGET_MS = 600_000L;

    public StaticAnalysisResult runAll(SourceBundle bundle, SelectedRules rules,
                                       AppConfig config, RuleMapping mapping,
                                       Path repoRoot) {
        Logs.module("M10");
        long t0 = System.currentTimeMillis();
        List<ToolRun> runs = new ArrayList<>();
        List<IssueCandidate> candidates = new ArrayList<>();

        CheckstyleRunner cs = new CheckstyleRunner();
        ToolRun csRun = cs.run(bundle, rules, config, mapping);
        runs.add(csRun);
        if (csRun.status() == ToolStatus.SUCCESS && csRun.rawOutputPath() != null) {
            candidates.addAll(cs.parseXml(csRun.rawOutputPath(), mapping));
        }

        if (System.currentTimeMillis() - t0 < TOTAL_BUDGET_MS) {
            PmdRunner pmd = new PmdRunner();
            ToolRun pmdRun = pmd.run(bundle, rules, config, mapping);
            runs.add(pmdRun);
            if (pmdRun.status() == ToolStatus.SUCCESS && pmdRun.rawOutputPath() != null) {
                candidates.addAll(pmd.parseXml(pmdRun.rawOutputPath(), mapping));
            }
        } else {
            runs.add(ToolRun.skipped(Tool.PMD, "7.4.0", "TOTAL_BUDGET_EXCEEDED"));
        }

        if (System.currentTimeMillis() - t0 < TOTAL_BUDGET_MS) {
            SpotbugsRunner sb = new SpotbugsRunner();
            ToolRun sbRun = sb.run(bundle, rules, config, mapping, repoRoot);
            runs.add(sbRun);
            if (sbRun.status() == ToolStatus.SUCCESS && sbRun.rawOutputPath() != null) {
                candidates.addAll(sb.parseXml(sbRun.rawOutputPath(), mapping));
            }
        } else {
            runs.add(ToolRun.skipped(Tool.SPOTBUGS, "4.8.6", "TOTAL_BUDGET_EXCEEDED"));
        }

        LOG.info("static analysis done runs={} candidates={}", runs.size(), candidates.size());
        return new StaticAnalysisResult(List.copyOf(runs), List.copyOf(candidates));
    }
}
