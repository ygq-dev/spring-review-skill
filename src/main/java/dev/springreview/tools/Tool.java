package dev.springreview.tools;

public enum Tool {
    CHECKSTYLE("checkstyle"),
    PMD("pmd"),
    SPOTBUGS("spotbugs"),
    CUSTOM("custom");

    private final String id;

    Tool(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Tool fromId(String s) {
        if (s == null) {
            return CUSTOM;
        }
        for (Tool t : values()) {
            if (t.id.equalsIgnoreCase(s)) {
                return t;
            }
        }
        return CUSTOM;
    }
}
