package dev.springreview.tools;

import dev.springreview.config.AppConfig;
import dev.springreview.observability.Logs;
import dev.springreview.rules.Rule;
import dev.springreview.rules.SelectedRules;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import dev.springreview.tools.xml.Xml;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * PMD 7.4.0 命令行调用。
 * 兼容 PMD 7 CLI：pmd check --file-list <file> -R <ruleset> -f xml -r <out> --use-version java-17
 */
public final class PmdRunner {

    private static final Logger LOG = Logs.logger("pmd");
    private static final String VERSION = "7.4.0";
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(120);

    public ToolRun run(SourceBundle bundle, SelectedRules rules,
                       AppConfig config, RuleMapping mapping) {
        ToolRun skip = precheck(bundle, rules, config);
        if (skip != null) {
            return skip;
        }
        Path exe = ToolPaths.pmdExecutable();
        if (exe == null) {
            LOG.info("pmd executable not found; SKIPPED");
            return ToolRun.skipped(Tool.PMD, VERSION, "TOOL_NOT_FOUND");
        }
        Path configXml = ToolPaths.pmdConfig();
        if (configXml == null) {
            LOG.info("pmd config not found; SKIPPED");
            return ToolRun.skipped(Tool.PMD, VERSION, "CONFIG_NOT_FOUND");
        }
        Instant start = Instant.now();
        long t0 = System.currentTimeMillis();
        Path tmpFileList = null;
        try {
            Path tmpOut = Files.createTempFile("pmd-", ".xml");
            Path tmpDir = tmpOut.getParent();
            List<String> files = collectJavaFiles(bundle);
            if (files.isEmpty()) {
                return ToolRun.skipped(Tool.PMD, VERSION, "NO_JAVA_FILES");
            }
            tmpFileList = Files.createTempFile("pmd-files-", ".txt");
            Files.write(tmpFileList, files, StandardCharsets.UTF_8);

            List<String> cmd = new ArrayList<>();
            cmd.add(exe.toString());
            cmd.add("check");
            cmd.add("--no-cache");
            cmd.add("--no-progress");
            cmd.add("--use-version");
            cmd.add("java-17");
            cmd.add("--file-list");
            cmd.add(tmpFileList.toString());
            cmd.add("-R");
            cmd.add(configXml.toString());
            cmd.add("-f");
            cmd.add("xml");
            cmd.add("-r");
            cmd.add(tmpOut.toString());

            ProcessRunner.Result r = new ProcessRunner().run(cmd, tmpDir, null, TIMEOUT_MS);
            Instant end = Instant.now();
            long dur = System.currentTimeMillis() - t0;
            if (r.timedOut()) {
                return new ToolRun(Tool.PMD, VERSION, ToolStatus.TIMEOUT,
                    start, end, dur, -1, 0, "TIMEOUT", null);
            }
            if (!Files.exists(tmpOut)) {
                return new ToolRun(Tool.PMD, VERSION, ToolStatus.FAILED,
                    start, end, dur, r.exitCode(), 0,
                    "NO_OUTPUT exit=" + r.exitCode() + " stderr=" + r.stderrTail(), null);
            }
            int count = parseXml(tmpOut, mapping).size();
            return new ToolRun(Tool.PMD, VERSION, ToolStatus.SUCCESS,
                start, end, dur, r.exitCode(), count, null, tmpOut);
        } catch (IOException ex) {
            LOG.error("pmd run failed: {}", ex.getMessage());
            Instant end = Instant.now();
            return new ToolRun(Tool.PMD, VERSION, ToolStatus.FAILED,
                start, end, System.currentTimeMillis() - t0, -1, 0,
                "IO_ERROR: " + ex.getMessage(), null);
        } finally {
            if (tmpFileList != null) {
                try {
                    Files.deleteIfExists(tmpFileList);
                } catch (IOException ignored) {
                    // 忽略清理失败
                }
            }
        }
    }

    public List<IssueCandidate> parseXml(Path xml, RuleMapping mapping) {
        List<IssueCandidate> out = new ArrayList<>();
        if (xml == null || !Files.exists(xml)) {
            return out;
        }
        try {
            Document doc = Xml.parse(xml);
            for (Element file : Xml.children(doc.getDocumentElement(), "file")) {
                String path = Xml.attr(file, "name");
                for (Element v : Xml.children(file, "violation")) {
                    int line = Xml.attrInt(v, "beginline");
                    int col = Xml.attrInt(v, "begincolumn");
                    String ruleName = Xml.attr(v, "rule");
                    String ruleset = Xml.attr(v, "ruleset");
                    String message = v.getTextContent() == null ? "" : v.getTextContent().trim();
                    Rule rule = mapping.find("PMD", ruleName);
                    if (rule == null) {
                        rule = mapping.find("PMD", ruleset + "/" + ruleName);
                    }
                    if (rule == null) {
                        continue;
                    }
                    out.add(new IssueCandidate(
                        rule.id(),
                        rule.severity(),
                        rule.confidence(),
                        "STATIC_ANALYSIS",
                        normalizePath(path),
                        line, col,
                        message,
                        message,
                        rule.remediation(),
                        "PMD",
                        ruleName,
                        "pmd:" + ruleset + "/" + ruleName));
                }
            }
        } catch (Exception ex) {
            LOG.warn("pmd xml parse failed: {}", ex.getMessage());
        }
        return out;
    }

    private ToolRun precheck(SourceBundle bundle, SelectedRules rules, AppConfig config) {
        if (!config.staticTools().pmd()) {
            return ToolRun.skipped(Tool.PMD, VERSION, "DISABLED");
        }
        boolean required = rules.rules().stream()
            .anyMatch(r -> "PMD".equalsIgnoreCase(r.tool()));
        if (!required) {
            return ToolRun.skipped(Tool.PMD, VERSION, "NO_RULES_SELECTED");
        }
        if (bundle.units().isEmpty()) {
            return ToolRun.skipped(Tool.PMD, VERSION, "NO_SOURCES");
        }
        return null;
    }

    private static List<String> collectJavaFiles(SourceBundle bundle) {
        List<String> out = new ArrayList<>();
        for (SourceUnit u : bundle.units()) {
            out.add(u.path());
        }
        return out;
    }

    private static String normalizePath(String p) {
        if (p == null) {
            return "";
        }
        return p.replace('\\', '/');
    }
}
