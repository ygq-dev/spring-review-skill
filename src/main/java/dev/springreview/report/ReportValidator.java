package dev.springreview.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import dev.springreview.exit.SpringReviewException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * M17：networknt 校验 A6 Schema。
 */
public final class ReportValidator {

    private final JsonSchema schema;

    public ReportValidator(Path schemaFile) {
        if (!Files.exists(schemaFile)) {
            throw SpringReviewException.report(
                "review-report.schema.json 不存在: " + schemaFile, null);
        }
        try {
            JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(schemaFile.toFile());
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012);
            this.schema = factory.getSchema(node);
        } catch (IOException ex) {
            throw SpringReviewException.report("读 Schema 失败: " + schemaFile, ex);
        }
    }

    public void validateOrThrow(JsonNode report) {
        Set<ValidationMessage> errors = schema.validate(report);
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (ValidationMessage m : errors) {
                if (n++ > 0) {
                    sb.append("; ");
                }
                sb.append(m.getMessage());
                if (n >= 5) {
                    sb.append(" ...");
                    break;
                }
            }
            throw SpringReviewException.report("报告 Schema 校验失败: " + sb, null);
        }
    }
}
