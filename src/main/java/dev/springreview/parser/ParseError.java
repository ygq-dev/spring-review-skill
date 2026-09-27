package dev.springreview.parser;

public record ParseError(
    String file,
    int line,
    int column,
    String message,
    ErrorType errorType,
    String severity,
    boolean recoverable,
    String sourceHash
) {

    public enum ErrorType {
        SYNTAX, ENCODING, LANGUAGE_LEVEL, IO, AST_LIMIT, INTERNAL, SKIP
    }

    public static ParseError syntax(String file, int line, int col, String msg, String hash) {
        return new ParseError(file, line, col, msg, ErrorType.SYNTAX, "ERROR", true, hash);
    }

    public static ParseError skip(String file, String msg, String hash) {
        return new ParseError(file, 0, 0, msg, ErrorType.SKIP, "INFO", true, hash);
    }

    public static ParseError limit(String file, String msg, String hash) {
        return new ParseError(file, 0, 0, msg, ErrorType.AST_LIMIT, "WARNING", true, hash);
    }

    public static ParseError internal(String file, String msg, String hash) {
        return new ParseError(file, 0, 0, msg, ErrorType.INTERNAL, "ERROR", false, hash);
    }
}
