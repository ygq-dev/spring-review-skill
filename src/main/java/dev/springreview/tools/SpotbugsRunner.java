package dev.springreview.tools;

import dev.springreview.config.AppConfig;
import dev.springreview.observability.Logs;
import dev.springreview.rules.Rule;
import dev.springreview.rules.SelectedRules;
import dev.springreview.scope.SourceBundle;
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

public final class SpotbugsRunner {

    private static final Logger LOG = Logs.logger("spotbugs");
    private static final String VERSION = "4.8.6";
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(180);

    public ToolRun run(SourceBundle bundle, SelectedRules rules,
                       AppConfig config, RuleMapping mapping,
                       Path repoRoot) {
        if (!config.staticTools().spotbugs()) {
            return ToolRun.skipped(Tool.SPOTBUGS, VERSION, "DISABLED");
        }
        boolean required = rules.rules().stream()
            .anyMatch(r -> "SPOTBUGS".equalsIgnoreCase(r.tool()));
        if (!required) {
            return ToolRun.skipped(Tool.SPOTBUGS, VERSION, "NO_RULES_SELECTED");
        }
        Path exe = ToolPaths.spotbugsExecutable();
        if (exe == null) {
            return ToolRun.skipped(Tool.SPOTBUGS, VERSION, "TOOL_NOT_FOUND");
        }
        Path classDir = repoRoot.resolve("target/classes");
        if (!Files.isDirectory(classDir)) {
            classDir = repoRoot.resolve("build/classes");
        }
        if (!Files.isDirectory(classDir)) {
            return ToolRun.skipped(Tool.SPOTBUGS, VERSION, "NO_COMPILED_ARTIFACTS");
        }
        Instant start = Instant.now();
        long t0 = System.currentTimeMillis();
        try {
            Path tmpOut = Files.createTempFile("spotbugs-", ".xml");
            Path tmpDir = tmpOut.getParent();
            List<String> cmd = new ArrayList<>();
            cmd.add(exe.toString());
            cmd.add("-xml:withMessages");
            cmd.add("-output");
            cmd.add(tmpOut.toString());
            cmd.add(classDir.toString());
            ProcessRunner.Result r = new ProcessRunner().run(cmd, tmpDir, null, TIMEOUT_MS);
            Instant end = Instant.now();
            long dur = System.currentTimeMillis() - t0;
            if (r.timedOut()) {
                return new ToolRun(Tool.SPOTBUGS, VERSION, ToolStatus.TIMEOUT,
                    start, end, dur, -1, 0, "TIMEOUT", null);
            }
            if (!Files.exists(tmpOut)) {
                return new ToolRun(Tool.SPOTBUGS, VERSION, ToolStatus.FAILED,
                    start, end, dur, r.exitCode(), 0,
                    "NO_OUTPUT exit=" + r.exitCode(), null);
            }
            int count = parseXml(tmpOut, mapping).size();
            return new ToolRun(Tool.SPOTBUGS, VERSION, ToolStatus.SUCCESS,
                start, end, dur, r.exitCode(), count, null, tmpOut);
        } catch (IOException ex) {
            LOG.error("spotbugs run failed: {}", ex.getMessage());
            Instant end = Instant.now();
            return new ToolRun(Tool.SPOTBUGS, VERSION, ToolStatus.FAILED,
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
            for (Element bug : Xml.children(doc.getDocumentElement(), "BugInstance")) {
                String type = Xml.attr(bug, "type");
                Rule rule = mapping.find("SPOTBUGS", type);
                if (rule == null) {
                    continue;
                }
                String message = "";
                List<Element> longMsgs = Xml.children(bug, "LongMessage");
                if (!longMsgs.isEmpty()) {
                    message = longMsgs.get(0).getTextContent();
                    if (message != null) {
                        message = message.trim();
                    }
                }
                int line = 0;
                int col = 0;
                String path = "";
                List<Element> sl = Xml.children(bug, "SourceLine");
                if (!sl.isEmpty()) {
                    Element s = sl.get(0);
                    path = Xml.attr(s, "sourcepath");
                    line = Xml.attrInt(s, "start");
                }
                out.add(new IssueCandidate(
                    rule.id(),
                    rule.severity(),
                    rule.confidence(),
                    "STATIC_ANALYSIS",
                    path == null ? "" : path.replace('\\', '/'),
                    line, col,
                    message,
                    message,
                    rule.remediation(),
                    "SPOTBUGS",
                    type,
                    "spotbugs:" + type));
            }
        } catch (Exception ex) {
            LOG.warn("spotbugs xml parse failed: {}", ex.getMessage());
        }
        return out;
    }
}
