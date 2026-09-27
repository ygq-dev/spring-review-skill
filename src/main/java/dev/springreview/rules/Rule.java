package dev.springreview.rules;

import java.util.List;
import java.util.Map;

/**
 * 规则视图，来自 rules/*.yaml 的只读投影。
 * 完整元数据保留在 RuleLoader 的 JsonNode 中，供后续阶段查询。
 */
public record Rule(
    String id,
    String name,
    String description,
    String categoryL1,
    String categoryL2,
    List<String> tags,
    String status,
    String severity,
    String confidence,
    String detectionMethod,
    String tool,
    String toolRuleId,
    Map<String, Object> parameters,
    boolean enabled,
    AppliesTo appliesTo,
    String message,
    String remediation,
    List<Source> sources,
    String ruleVersion,
    String metadataVersion,
    String schemaVersion,
    String owner,
    List<String> testCases
) {

    public record AppliesTo(
        String javaVersion,
        String springBootVersion,
        List<String> buildTool,
        List<String> moduleType,
        List<String> framework,
        List<String> exclude
    ) {
        public boolean appliesToAllModules() {
            return moduleType.isEmpty() || moduleType.contains("all");
        }
    }

    public record Source(String type, String ref, String version) {
    }
}
