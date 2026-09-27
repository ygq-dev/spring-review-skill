package dev.springreview.report;

import dev.springreview.observability.Logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 生成 report_id：RPT-YYYY-NNNN。
 * 同日按 history/YYYY/MM/DD/ 下已有的 report 目录数 + 1。
 */
public final class ReportIdGenerator {

    private static final Pattern RPT = Pattern.compile("^RPT-\\d{4}-\\d{4}$");

    public String next(Instant now, Path reportsRoot) {
        int year = now.atZone(ZoneOffset.UTC).getYear();
        String yearStr = String.valueOf(year);
        Path dayDir = reportsRoot.resolve("history")
            .resolve(yearStr)
            .resolve(DateTimeFormatter.ofPattern("MM").format(
                now.atZone(ZoneOffset.UTC)))
            .resolve(DateTimeFormatter.ofPattern("dd").format(
                now.atZone(ZoneOffset.UTC)));
        int existing = 0;
        if (Files.isDirectory(dayDir)) {
            try (var stream = Files.list(dayDir)) {
                existing = (int) stream
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> RPT.matcher(name).matches())
                    .count();
            } catch (IOException ex) {
                Logs.logger("report-id").warn("scan dayDir failed: {}", ex.getMessage());
            }
        }
        int seq = existing + 1;
        return String.format("RPT-%s-%04d", yearStr, seq);
    }
}
