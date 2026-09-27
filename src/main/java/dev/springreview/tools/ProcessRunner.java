package dev.springreview.tools;

import dev.springreview.observability.Logs;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 命令行进程执行器：超时杀进程树；输出重定向到文件。
 */
public final class ProcessRunner {

    private static final Logger LOG = Logs.logger("process-runner");

    public record Result(int exitCode, boolean timedOut, long durationMs, String stderrTail) {
    }

    public Result run(List<String> command, Path workingDir, Path stdoutFile, long timeoutMs) {
        long start = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }
        pb.redirectErrorStream(false);
        try {
            if (stdoutFile != null) {
                pb.redirectOutput(stdoutFile.toFile());
            } else {
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            }
            Path stderrFile = Files.createTempFile("srs-stderr-", ".log");
            pb.redirectError(stderrFile.toFile());
            Process p = pb.start();
            boolean finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroy();
                if (!p.waitFor(2, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
                String tail = readTail(stderrFile, 8192);
                long dur = System.currentTimeMillis() - start;
                LOG.warn("process timeout after {}ms: {}", dur, String.join(" ", command));
                return new Result(-1, true, dur, tail);
            }
            int exit = p.exitValue();
            String tail = readTail(stderrFile, 8192);
            long dur = System.currentTimeMillis() - start;
            return new Result(exit, false, dur, tail);
        } catch (IOException ex) {
            LOG.error("process start failed: {}", ex.getMessage());
            long dur = System.currentTimeMillis() - start;
            return new Result(-1, false, dur, "start failed: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            long dur = System.currentTimeMillis() - start;
            return new Result(-1, false, dur, "interrupted");
        }
    }

    private static String readTail(Path file, int maxBytes) {
        try {
            if (!Files.exists(file)) {
                return "";
            }
            byte[] all = Files.readAllBytes(file);
            int start = Math.max(0, all.length - maxBytes);
            return new String(all, start, all.length - start, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    @SuppressWarnings("unused")
    private static File dummy() {
        return new File(".");
    }
}
