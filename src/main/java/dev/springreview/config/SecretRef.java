package dev.springreview.config;

/**
 * 密钥引用，不含明文。
 */
public record SecretRef(
        String name,
        String provider,
        Source source,
        String envVarName,
        boolean present,
        String redactedValue
) {
    public enum Source {
        ENV, CI_SECRET, DOTENV_LOCAL, NONE
    }

    public static SecretRef missing(String name, String provider, String envVarName) {
        return new SecretRef(name, provider, Source.NONE, envVarName, false, "***");
    }

    public static SecretRef present(String name, String provider, Source source, String envVarName) {
        return new SecretRef(name, provider, source, envVarName, true, "***");
    }
}