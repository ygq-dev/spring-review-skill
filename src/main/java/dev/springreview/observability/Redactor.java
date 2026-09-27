package dev.springreview.observability;

import java.util.regex.Pattern;

/**
 * 脱敏工具：null 安全、幂等、不抛异常。
 * 覆盖：私钥块、Bearer、敏感 key=value、URL userinfo。
 */
public final class Redactor {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern PRIVATE_KEY = Pattern.compile(
        "-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----");

    private static final Pattern BEARER = Pattern.compile(
        "(?i)(\\bBearer\\s+)([A-Za-z0-9._\\-]+)");

    private static final Pattern KEY_VALUE = Pattern.compile(
        "(?i)(\\b(?:api[_-]?key|apikey|secret|token|password|passwd|pwd"
            + "|authorization|private[_-]?key|privatekey|credential"
            + "|access[_-]?key|cookie|session)\\s*[=:]\\s*)"
            + "(['\"]?)([^\\s'\",}]+)(['\"]?)");

    private static final Pattern URL_USERINFO = Pattern.compile(
        "(?i)([a-z][a-z0-9+.-]*://)([^/@\\s:]+):([^/@\\s]+)@");

    private Redactor() {
    }

    public static String redact(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            String out = value;
            out = PRIVATE_KEY.matcher(out).replaceAll(REDACTED);
            out = BEARER.matcher(out).replaceAll(mr -> mr.group(1) + REDACTED);
            out = URL_USERINFO.matcher(out).replaceAll(mr -> mr.group(1) + REDACTED + "@");
            out = KEY_VALUE.matcher(out).replaceAll(
                mr -> mr.group(1) + mr.group(2) + REDACTED + mr.group(4));
            return out;
        } catch (Throwable t) {
            return REDACTED;
        }
    }
}
