package dev.springreview.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import dev.springreview.exit.SpringReviewException;
import dev.springreview.observability.Logs;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * M07：以 rules/index.json 为唯一入口加载 rules/*.yaml；
 * 多文档 YAML → JsonNode → A5 Schema 校验 → 索引一致性校验 → 状态过滤。
 * 加载/元数据校验失败退出 4。空规则集不致命但 WARN。
 */
public final class RuleLoader {

    private static final Logger LOG = Logs.logger("rule-loader");

    public RuleSet load(Path rulesDir, Path indexFile, Path schemaFile, Path workingDir) {
        Logs.module("M07");
        if (!Files.exists(rulesDir)) {
            throw SpringReviewException.ruleLoad("rulesDir 不存在: " + rulesDir, null);
        }
        if (!Files.exists(indexFile)) {
            throw SpringReviewException.ruleLoad("rules/index.json 不存在: " + indexFile, null);
        }
        if (!Files.exists(schemaFile)) {
            throw SpringReviewException.ruleLoad(
                "rule-metadata.schema.json 不存在: " + schemaFile, null);
        }

        JsonNode indexNode = readJson(indexFile);
        JsonSchema schema = loadSchema(schemaFile);

        JsonNode itemsNode = indexNode.get("items");
        if (itemsNode == null || !itemsNode.isArray()) {
            throw SpringReviewException.ruleLoad("index.items 非数组", null);
        }
        String indexVersion = indexNode.path("version").asText("unknown");
        int count = indexNode.path("count").asInt(-1);
        if (count >= 0 && count != itemsNode.size()) {
            throw SpringReviewException.ruleLoad(
                "index.count=" + count + " 与 items.length=" + itemsNode.size() + " 不一致", null);
        }

        List<JsonNode> items = new ArrayList<>();
        itemsNode.forEach(items::add);
        if (items.isEmpty()) {
            LOG.warn("rules index empty");
            return new RuleSet(List.of(), indexVersion, Instant.now(),
                rulesDir.toString(), List.of(), Map.of());
        }

        Set<String> seenIds = new HashSet<>();
        for (JsonNode item : items) {
            String id = item.path("id").asText(null);
            if (id == null || id.isBlank()) {
                throw SpringReviewException.ruleLoad("index items 缺少 id", null);
            }
            if (!seenIds.add(id)) {
                throw SpringReviewException.ruleLoad("index items id 重复: " + id, null);
            }
        }

        Map<String, List<JsonNode>> itemsByFile = new LinkedHashMap<>();
        for (JsonNode item : items) {
            String file = item.path("file").asText(null);
            if (file == null || file.isBlank()) {
                throw SpringReviewException.ruleLoad(
                    "index items[].file 缺失 for id=" + item.path("id").asText(), null);
            }
            itemsByFile.computeIfAbsent(file, k -> new ArrayList<>()).add(item);
        }

        Map<String, JsonNode> ruleNodeById = new LinkedHashMap<>();
        List<RuleSet.Diagnostic> diagnostics = new ArrayList<>();

        for (Map.Entry<String, List<JsonNode>> e : itemsByFile.entrySet()) {
            Path yamlPath = resolveYamlPath(e.getKey(), rulesDir, workingDir);
            if (yamlPath == null || !Files.exists(yamlPath)) {
                throw SpringReviewException.ruleLoad(
                    "索引指向的 YAML 不存在: " + e.getKey(), null);
            }
            List<JsonNode> docs;
            try {
                docs = readMultiDocYaml(yamlPath);
            } catch (IOException ex) {
                throw SpringReviewException.ruleLoad("YAML 解析失败: " + yamlPath, ex);
            }
            if (docs.size() != e.getValue().size()) {
                throw SpringReviewException.ruleLoad(
                    "YAML " + yamlPath + " 文档数 " + docs.size()
                        + " 与索引条目数 " + e.getValue().size() + " 不一致", null);
            }
            for (int i = 0; i < docs.size(); i++) {
                JsonNode doc = docs.get(i);
                String id = doc.path("rule_id").asText(null);
                if (id == null) {
                    throw SpringReviewException.ruleLoad(
                        "YAML " + yamlPath + " 第 " + (i + 1) + " 个文档缺少 rule_id", null);
                }
                if (ruleNodeById.containsKey(id)) {
                    throw SpringReviewException.ruleLoad("YAML 中 rule_id 重复: " + id, null);
                }
                ruleNodeById.put(id, doc);

                Set<ValidationMessage> errors = schema.validate(doc);
                if (!errors.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int n = 0;
                    for (ValidationMessage m : errors) {
                        if (n++ > 0) {
                            sb.append("; ");
                        }
                        sb.append(m.getMessage());
                    }
                    diagnostics.add(new RuleSet.Diagnostic(
                        yamlPath.toString(), i + 1, id, "ERROR", sb.toString()));
                }
            }
        }

        if (diagnostics.stream().anyMatch(d -> "ERROR".equals(d.level()))) {
            for (RuleSet.Diagnostic d : diagnostics) {
                LOG.error("rule schema error file={} doc={} id={} msg={}",
                    d.file(), d.docIndex(), d.ruleId(), d.message());
            }
            throw SpringReviewException.ruleLoad(
                "规则元数据 Schema 校验失败，共 " + diagnostics.size() + " 条", null);
        }

        List<Rule> activeRules = new ArrayList<>();
        Map<String, Rule> byId = new LinkedHashMap<>();
        Set<String> indexedIds = new HashSet<>();
        for (JsonNode item : items) {
            String id = item.path("id").asText();
            indexedIds.add(id);
            JsonNode ruleNode = ruleNodeById.get(id);
            if (ruleNode == null) {
                throw SpringReviewException.ruleLoad("索引有 " + id + " 但 YAML 缺", null);
            }
            checkConsistency(item, ruleNode, id);
            Rule rule = toRule(ruleNode);
            byId.put(id, rule);
            if ("ACTIVE".equalsIgnoreCase(rule.status())) {
                activeRules.add(rule);
            }
        }
        for (String id : ruleNodeById.keySet()) {
            if (!indexedIds.contains(id)) {
                throw SpringReviewException.ruleLoad("YAML 有 " + id + " 但索引缺", null);
            }
        }

        LOG.info("rules loaded total={} active={} indexVersion={}",
            byId.size(), activeRules.size(), indexVersion);
        return new RuleSet(
            List.copyOf(activeRules),
            indexVersion,
            Instant.now(),
            rulesDir.toString(),
            List.copyOf(diagnostics),
            Map.copyOf(byId)
        );
    }

    // ---------- 一致性 ----------

    private static void checkConsistency(JsonNode item, JsonNode rule, String id) {
        requireEqual(item, "category", rule, "category_l1", id);
        requireEqual(item, "subcategory", rule, "category_l2", id);
        requireEqual(item, "severity", rule, "severity", id);
        requireEqual(item, "status", rule, "status", id);
        requireEqual(item, "version", rule, "rule_version", id);
    }

    private static void requireEqual(JsonNode index, String indexKey,
                                     JsonNode rule, String ruleKey, String id) {
        String a = index.path(indexKey).asText("");
        String b = rule.path(ruleKey).asText("");
        if (!a.isEmpty() && !a.equals(b)) {
            throw SpringReviewException.ruleLoad(
                indexKey + " 不一致 for " + id + ": index=" + a + " yaml=" + b, null);
        }
    }

    // ---------- IO ----------

    private static Path resolveYamlPath(String file, Path rulesDir, Path workingDir) {
        Path p = rulesDir.resolve(file);
        if (Files.exists(p)) {
            return p;
        }
        Path fileName = Path.of(file).getFileName();
        if (fileName != null) {
            p = rulesDir.resolve(fileName);
            if (Files.exists(p)) {
                return p;
            }
        }
        if (workingDir != null) {
            p = workingDir.resolve(file);
            if (Files.exists(p)) {
                return p;
            }
        }
        return null;
    }

    private static JsonNode readJson(Path p) {
        try {
            return new ObjectMapper().readTree(p.toFile());
        } catch (IOException ex) {
            throw SpringReviewException.ruleLoad("读 index 失败: " + p, ex);
        }
    }

    private static JsonSchema loadSchema(Path p) {
        try {
            JsonNode schemaNode = new ObjectMapper().readTree(p.toFile());
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012);
            return factory.getSchema(schemaNode);
        } catch (IOException ex) {
            throw SpringReviewException.ruleLoad("读 Schema 失败: " + p, ex);
        }
    }

    private static List<JsonNode> readMultiDocYaml(Path p) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        List<JsonNode> docs = new ArrayList<>();
        try (InputStream in = Files.newInputStream(p);
             MappingIterator<JsonNode> it = yamlMapper.readerFor(JsonNode.class).readValues(in)) {
            while (it.hasNext()) {
                JsonNode doc = it.next();
                if (doc != null && !doc.isMissingNode() && !doc.isNull()) {
                    docs.add(doc);
                }
            }
        }
        return docs;
    }

    // ---------- 投影 ----------

    private static Rule toRule(JsonNode n) {
        JsonNode at = n.path("applies_to");
        Rule.AppliesTo appliesTo = new Rule.AppliesTo(
            at.path("java_version").asText(""),
            at.path("spring_boot_version").asText(""),
            toStringList(at.path("build_tool")),
            toStringList(at.path("module_type")),
            toStringList(at.path("framework")),
            toStringList(at.path("exclude"))
        );
        List<Rule.Source> sources = new ArrayList<>();
        for (JsonNode s : n.path("sources")) {
            sources.add(new Rule.Source(
                s.path("type").asText(""),
                s.path("ref").asText(""),
                s.path("version").asText("")
            ));
        }
        String toolRuleId = n.has("tool_rule_id") && !n.path("tool_rule_id").isNull()
            ? n.path("tool_rule_id").asText() : null;

        // 修复：递归转换 parameters 中的 JSON 节点为 Java 对象
        Map<String, Object> params = new LinkedHashMap<>();
        JsonNode pn = n.path("parameters");
        if (pn.isObject()) {
            pn.fields().forEachRemaining(e ->
                params.put(e.getKey(), jsonToJava(e.getValue())));
        }

        return new Rule(
            n.path("rule_id").asText(),
            n.path("name").asText(""),
            n.path("description").asText(""),
            n.path("category_l1").asText(""),
            n.path("category_l2").asText(""),
            toStringList(n.path("tags")),
            n.path("status").asText(""),
            n.path("severity").asText(""),
            n.path("confidence").asText(""),
            n.path("detection_method").asText(""),
            n.path("tool").asText(""),
            toolRuleId,
            Map.copyOf(params),
            n.path("enabled").asBoolean(true),
            appliesTo,
            n.path("message").asText(""),
            n.path("remediation").asText(""),
            List.copyOf(sources),
            n.path("rule_version").asText(""),
            n.path("metadata_version").asText(""),
            n.path("schema_version").asText(""),
            n.path("owner").asText(""),
            toStringList(n.path("test_cases"))
        );
    }

    /**
     * 把 Jackson JsonNode 递归转换为 Java 对象：
     * Textual/Number/Boolean → String/Integer/Double/Boolean
     * Array → List<Object>
     * Object → Map<String, Object>
     */
    private static Object jsonToJava(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isInt()) {
            return node.asInt();
        }
        if (node.isLong()) {
            return node.asLong();
        }
        if (node.isDouble() || node.isFloat()) {
            return node.asDouble();
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(child -> list.add(jsonToJava(child)));
            return list;
        }
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.fields().forEachRemaining(e -> map.put(e.getKey(), jsonToJava(e.getValue())));
            return map;
        }
        return node.asText();
    }

    private static List<String> toStringList(JsonNode n) {
        if (n == null || !n.isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        n.forEach(x -> out.add(x.asText()));
        return List.copyOf(out);
    }
}
