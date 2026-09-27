package dev.springreview.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.springreview.config.AppConfig;
import dev.springreview.exit.SpringReviewException;
import dev.springreview.observability.Logs;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * M16：写盘与归档。
 * - 输出 JSON + Markdown
 * - latest 原子替换
 * - history/YYYY/MM/DD/<report_id>/
 * - Windows 下强制 UTF-8
 */
public final class ReportWriter {

    private static final Logger LOG = Logs.logger("report-writer");
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    public record OutputArtifacts(
        String reportId,
        Path latestJson,
        Path latestMarkdown,
        Path historyDir,
        Path historyJson,
        Path historyMarkdown,
        Instant writtenAt
    ) {
    }

    public OutputArtifacts write(JsonNode reportJson, String markdown,
                                 AppConfig config, Instant generatedAt) {
        Logs.module("M16");
        String reportId = reportJson.path("report_id").asText("RPT-0000-0000");
        Path outputDir = config.output().dir().toAbsolutePath().normalize();
        Path latestDir = config.output().latestDir().toAbsolutePath().normalize();
        Path historyDir = resolveHistoryDir(outputDir, config.output().historyPattern(),
            reportId, generatedAt);

        try {
            Files.createDirectories(latestDir);
            Files.createDirectories(historyDir);
        } catch (IOException ex) {
            throw SpringReviewException.report("创建输出目录失败", ex);
        }

        String jsonText;
        try {
            jsonText = MAPPER.writeValueAsString(reportJson);
        } catch (JsonProcessingException ex) {
            throw SpringReviewException.report("JSON 序列化失败", ex);
        }

        // history 先写
        Path historyJson = historyDir.resolve("review-report.json");
        Path historyMd = historyDir.resolve("review-report.md");
        atomicWriteUtf8(historyJson, jsonText);
        if (markdown != null) {
            atomicWriteUtf8(historyMd, markdown);
        }

        // latest 原子替换
        Path latestJson = latestDir.resolve("review-report.json");
        Path latestMd = latestDir.resolve("review-report.md");
        atomicWriteUtf8(latestJson, jsonText);
        if (markdown != null) {
            atomicWriteUtf8(latestMd, markdown);
        }

        LOG.info("report written reportId={} latestJson={} historyDir={}",
            reportId, latestJson, historyDir);
        return new OutputArtifacts(reportId, latestJson, latestMd, historyDir,
            historyJson, historyMd, Instant.now());
    }

    private static Path resolveHistoryDir(Path outputDir, String pattern,
                                          String reportId, Instant generatedAt) {
        String yyyy = DateTimeFormatter.ofPattern("yyyy").format(generatedAt.atZone(ZoneOffset.UTC));
        String mm = DateTimeFormatter.ofPattern("MM").format(generatedAt.atZone(ZoneOffset.UTC));
        String dd = DateTimeFormatter.ofPattern("dd").format(generatedAt.atZone(ZoneOffset.UTC));
        if (pattern == null || pattern.isBlank()) {
            return outputDir.resolve("history").resolve(yyyy).resolve(mm).resolve(dd).resolve(reportId);
        }
        String resolved = pattern
            .replace("YYYY", yyyy)
            .replace("MM", mm)
            .replace("DD", dd)
            .replace("<report_id>", reportId);
        return outputDir.resolve(resolved);
    }

    private static void atomicWriteUtf8(Path target, String content) {
        try {
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Path tmp = target.resolveSibling(
                target.getFileName() + ".tmp-" + UUID.randomUUID());
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw SpringReviewException.report("写文件失败: " + target, ex);
        }
    }
}
