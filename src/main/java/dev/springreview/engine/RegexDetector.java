package dev.springreview.engine;

import dev.springreview.observability.Logs;
import dev.springreview.parser.ParsedBundle;
import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import dev.springreview.tools.IssueCandidate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 通用正则检测器。
 * - 支持 parameters.patterns / parameters.pattern
 * - 支持 parameters.exclude_line_contains：命中行若含任一关键字则跳过
 * - 限流：单文件单规则最多 10 条候选
 */
public final class RegexDetector implements Detector {

    private static final Logger LOG = Logs.logger("regex-detector");
    private static final int MAX_PER_FILE_PER_RULE = 10;

    @Override
    public boolean supports(Rule rule) {
        return "REGEX".equalsIgnoreCase(rule.detectionMethod()) && hasPattern(rule);
    }

    @Override
    public List<IssueCandidate> detect(Rule rule, SourceBundle bundle, ParsedBundle parsed) {
        List<IssueCandidate> out = new ArrayList<>();
        List<Pattern> patterns = compile(rule);
        if (patterns.isEmpty()) {
            return out;
        }
        List<String> excludeContains = listParam(rule, "exclude_line_contains");

        for (SourceUnit u : bundle.units()) {
            if (u.content() == null || u.content().isEmpty()) {
                continue;
            }
            String filtered = stripCommentLines(u.content());
            int perFileHits = 0;
            for (Pattern p : patterns) {
                if (perFileHits >= MAX_PER_FILE_PER_RULE) {
                    break;
                }
                Matcher m = p.matcher(filtered);
                while (m.find()) {
                    if (perFileHits >= MAX_PER_FILE_PER_RULE) {
                        break;
                    }
                    int[] lc = offsetToLineColumn(filtered, m.start());
                    String lineText = lineAt(filtered, lc[0]);
                    if (lineMatchesExclude(lineText, excludeContains)) {
                        continue;
                    }
                    String evidence = safeSubstring(filtered, m.start(), m.end());
                    if (evidence == null || evidence.isBlank()) {
                        evidence = rule.id() + " match";
                    }
                    out.add(new IssueCandidate(
                        rule.id(), rule.severity(), rule.confidence(),
                        "REGEX", u.path(), lc[0], lc[1],
                        evidence, rule.message(), rule.remediation(),
                        "CUSTOM", rule.toolRuleId(),
                        "regex:" + p.pattern()));
                    perFileHits++;
                }
            }
        }
        return out;
    }

    /** 把行注释（// ...）和块注释（/* ... *\/）内容替换为空格，保持行号与列号。 */
    private static String stripCommentLines(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        boolean inBlock = false;
        boolean inLine = false;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = (i + 1 < text.length()) ? text.charAt(i + 1) : '\0';

            if (inLine) {
                if (c == '\n') {
                    inLine = false;
                    sb.append(c);
                } else {
                    sb.append(' ');
                }
                continue;
            }
            if (inBlock) {
                if (c == '*' && next == '/') {
                    sb.append(' ');
                    sb.append(' ');
                    i++;
                    inBlock = false;
                } else if (c == '\n') {
                    sb.append(c);
                } else {
                    sb.append(' ');
                }
                continue;
            }
            if (inString) {
                sb.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (inChar) {
                sb.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '\'') {
                    inChar = false;
                }
                continue;
            }
            // 非注释、非字符串状态
            if (c == '/' && next == '/') {
                sb.append(' ');
                sb.append(' ');
                i++;
                inLine = true;
                continue;
            }
            if (c == '/' && next == '*') {
                sb.append(' ');
                sb.append(' ');
                i++;
                inBlock = true;
                continue;
            }
            if (c == '"') {
                inString = true;
                sb.append(c);
                continue;
            }
            if (c == '\'') {
                inChar = true;
                sb.append(c);
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String lineAt(String text, int lineNo) {
        if (lineNo <= 0) {
            return "";
        }
        int cur = 1;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (cur == lineNo) {
                start = i;
                break;
            }
            if (text.charAt(i) == '\n') {
                cur++;
            }
        }
        if (cur != lineNo) {
            return "";
        }
        int end = text.indexOf('\n', start);
        if (end < 0) {
            end = text.length();
        }
        return text.substring(start, end);
    }

    private static boolean lineMatchesExclude(String line, List<String> excludeContains) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        for (String kw : excludeContains) {
            if (line.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPattern(Rule rule) {
        return !compile(rule).isEmpty();
    }

    private static List<Pattern> compile(Rule rule) {
        List<Pattern> out = new ArrayList<>();
        Object v = rule.parameters() == null ? null : rule.parameters().get("patterns");
        if (v == null) {
            v = rule.parameters() == null ? null : rule.parameters().get("pattern");
        }
        if (v == null) {
            return out;
        }
        List<String> strs = new ArrayList<>();
        if (v instanceof List<?> list) {
            for (Object o : list) {
                strs.add(String.valueOf(o));
            }
        } else {
            strs.add(String.valueOf(v));
        }
        for (String s : strs) {
            try {
                Pattern p = Pattern.compile(s);
                if (p.matcher("").find()) {
                    LOG.warn("rule {} pattern matches empty string, skipped: {}",
                        rule.id(), s);
                    continue;
                }
                out.add(p);
            } catch (PatternSyntaxException ex) {
                LOG.warn("bad regex in rule {}: {}", rule.id(), ex.getMessage());
            }
        }
        return out;
    }

    private static List<String> listParam(Rule rule, String key) {
        if (rule.parameters() == null) {
            return List.of();
        }
        Object v = rule.parameters().get(key);
        if (v == null) {
            return List.of();
        }
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
            return out;
        }
        return List.of(String.valueOf(v));
    }

    private static int[] offsetToLineColumn(String text, int offset) {
        int line = 1;
        int col = 1;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        return new int[]{line, col};
    }

    private static String safeSubstring(String text, int start, int end) {
        int e = Math.min(end, start + 200);
        if (start < 0 || e > text.length()) {
            return "";
        }
        return text.substring(start, e).replaceAll("\\s+", " ").trim();
    }
}
