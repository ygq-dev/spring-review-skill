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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class CheckstyleRunner {

    private static final Logger LOG = Logs.logger("checkstyle");
    private static final String VERSION = "10.17.0";
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(60);

    public ToolRun run(SourceBundle bundle, SelectedRules rules,
                       AppConfig config, RuleMapping mapping) {
        ToolRun skip = precheck(bundle, rules, config);
        if (skip != null) {
            return skip;
        }
        Path jar = ToolPaths.checkstyleJar();
        if (jar == null) {
            LOG.info("checkstyle jar not found; SKIPPED");
            return ToolRun.skipped(Tool.CHECKSTYLE, VERSION, "TOOL_NOT_FOUND");
        }
        Path configXml = ToolPaths.checkstyleConfig();
        if (configXml == null) {
            LOG.info("checkstyle config not found; SKIPPED");
            return ToolRun.skipped(Tool.CHECKSTYLE, VERSION, "CONFIG_NOT_FOUND");
        }
        Instant start = Instant.now();
        long t0 = System.currentTimeMillis();
        try {
            Path tmpOut = Files.createTempFile("checkstyle-", ".xml");
            Path tmpDir = tmpOut.getParent();
            List<String> files = collectJavaFiles(bundle);
            if (files.isEmpty()) {
                return ToolRun.skipped(Tool.CHECKSTYLE, VERSION, "NO_JAVA_FILES");
            }
            List<String> cmd = new ArrayList<>();
            cmd.add(javaBin());
            cmd.add("-Dfile.encoding=UTF-8");
            cmd.add("-jar");
            cmd.add(jar.toString());
            cmd.add("-c");
            cmd.add(configXml.toString());
            cmd.add("-f");
            cmd.add("xml");
            cmd.add("-o");
            cmd.add(tmpOut.toString());
            cmd.addAll(files);

            ProcessRunner.Result r = new ProcessRunner().run(cmd, tmpDir, null, TIMEOUT_MS);
            Instant end = Instant.now();
            long dur = System.currentTimeMillis() - t0;
            if (r.timedOut()) {
                return new ToolRun(Tool.CHECKSTYLE, VERSION, ToolStatus.TIMEOUT,
                    start, end, dur, -1, 0, "TIMEOUT", null);
            }
            if (!Files.exists(tmpOut)) {
                return new ToolRun(Tool.CHECKSTYLE, VERSION, ToolStatus.FAILED,
                    start, end, dur, r.exitCode(), 0,
                    "NO_OUTPUT exit=" + r.exitCode(), null);
            }
            int count = parseXml(tmpOut, mapping).size();
            return new ToolRun(Tool.CHECKSTYLE, VERSION, ToolStatus.SUCCESS,
                start, end, dur, r.exitCode(), count, null, tmpOut);
        } catch (IOException ex) {
            LOG.error("checkstyle run failed: {}", ex.getMessage());
            Instant end = Instant.now();
            return new ToolRun(Tool.CHECKSTYLE, VERSION, ToolStatus.FAILED,
                start, end, System.currentTimeMillis() - t0, -1, 0,
                "IO_ERROR: " + ex.getMessage(), null);
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
                for (Element err : Xml.children(file, "error")) {
                    int line = Xml.attrInt(err, "line");
                    int col = Xml.attrInt(err, "column");
                    String severity = Xml.attr(err, "severity");
                    String message = Xml.attr(err, "message");
                    String source = Xml.attr(err, "source");
                    Rule rule = mapping.find("CHECKSTYLE", source);
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
                        "CHECKSTYLE",
                        source,
                        "checkstyle:" + severity + ":" + source));
                }
            }
        } catch (Exception ex) {
            LOG.warn("checkstyle xml parse failed: {}", ex.getMessage());
        }
        return out;
    }

    private ToolRun precheck(SourceBundle bundle, SelectedRules rules, AppConfig config) {
        if (!config.staticTools().checkstyle()) {
            return ToolRun.skipped(Tool.CHECKSTYLE, VERSION, "DISABLED");
        }
        boolean required = rules.rules().stream()
            .anyMatch(r -> "CHECKSTYLE".equalsIgnoreCase(r.tool()));
        if (!required) {
            return ToolRun.skipped(Tool.CHECKSTYLE, VERSION, "NO_RULES_SELECTED");
        }
        if (bundle.units().isEmpty()) {
            return ToolRun.skipped(Tool.CHECKSTYLE, VERSION, "NO_SOURCES");
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

    private static String javaBin() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path p = Path.of(javaHome, "bin", "java");
            if (Files.exists(p)) {
                return p.toString();
            }
            Path bat = Path.of(javaHome, "bin", "java.exe");
            if (Files.exists(bat)) {
                return bat.toString();
            }
        }
        return "java";
    }

    private static String normalizePath(String p) {
        if (p == null) {
            return "";
        }
        return p.replace('\\', '/');
    }
}
