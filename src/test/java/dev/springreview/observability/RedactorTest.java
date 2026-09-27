package dev.springreview.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedactorTest {

    @Test
    void nullAndEmpty_areReturnedAsIs() {
        assertThat(Redactor.redact(null)).isNull();
        assertThat(Redactor.redact("")).isEmpty();
    }

    @Test
    void bearerToken_isRedacted() {
        String in = "Authorization: Bearer abcDEF123.xyz-_.+=";
        String out = Redactor.redact(in);
        assertThat(out).contains("[REDACTED]");
        assertThat(out).doesNotContain("abcDEF123");
    }

    @Test
    void passwordKeyValue_isRedacted() {
        String out = Redactor.redact("password=admin123 secret:xyz token=\"tok-abc\"");
        assertThat(out).contains("[REDACTED]");
        assertThat(out).doesNotContain("admin123");
        assertThat(out).doesNotContain("xyz");
        assertThat(out).doesNotContain("tok-abc");
    }

    @Test
    void apiKeyVariants_areRedacted() {
        String[] keys = {
                "api_key=secret-value",
                "apikey=secret-value",
                "api-key=secret-value",
                "access_key=secret-value",
                "private_key=secret-value",
        };
        for (String k : keys) {
            assertThat(Redactor.redact(k)).contains("[REDACTED]");
        }
    }

    @Test
    void urlUserinfo_isRedacted() {
        String out = Redactor.redact("https://user:pw@example.com/path");
        assertThat(out).contains("[REDACTED]@");
        assertThat(out).doesNotContain("user:pw");
    }

    @Test
    void privateKeyBlock_isFullyRedacted() {
        String pem = "-----BEGIN PRIVATE KEY-----\nABCDEF\n-----END PRIVATE KEY-----";
        String out = Redactor.redact("key:" + pem);
        assertThat(out).doesNotContain("ABCDEF");
        assertThat(out).contains("[REDACTED]");
    }

    @Test
    void idempotent() {
        String in = "password=admin123";
        String once = Redactor.redact(in);
        String twice = Redactor.redact(once);
        assertThat(twice).isEqualTo(once);
    }

    @Test
    void nonSensitiveText_isUnchanged() {
        assertThat(Redactor.redact("hello world")).isEqualTo("hello world");
    }
}