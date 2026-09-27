package dev.springreview.rules;

import dev.springreview.exit.SpringReviewException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：真实 rules/ 目录存在时才跑。
 * CI 上如未提供 rules/*.yaml 与 index.json，会 skip。
 */
class RuleLoaderIntegrationTest {

    private static final Path RULES_DIR = Path.of("rules");
    private static final Path INDEX = RULES_DIR.resolve("index.json");
    private static final Path SCHEMA = Path.of("schema/rule-metadata.schema.json");

    @Test
    void loadsRealRulesIndex() {
        Assumptions.assumeTrue(Files.exists(INDEX),
            "rules/index.json 不存在，跳过集成测试");
        Assumptions.assumeTrue(Files.exists(SCHEMA),
            "schema/rule-metadata.schema.json 不存在，跳过集成测试");

        RuleLoader loader = new RuleLoader();
        RuleSet set = loader.load(RULES_DIR, INDEX, SCHEMA, Path.of("."));

        assertThat(set.rules()).isNotEmpty();
        assertThat(set.indexVersion()).isNotBlank();
        assertThat(set.byId()).isNotEmpty();
        // 索引 items 顺序继承
        assertThat(set.rules()).allMatch(r -> "ACTIVE".equalsIgnoreCase(r.status()));
    }

    @Test
    void missingIndexThrows() {
        RuleLoader loader = new RuleLoader();
        try {
            loader.load(Path.of("rules"), Path.of("rules/__not_exist__.json"),
                Path.of("schema/rule-metadata.schema.json"), Path.of("."));
            throw new AssertionError("expected exception");
        } catch (SpringReviewException expected) {
            assertThat(expected.exitCode()).isEqualTo(4);
        }
    }
}
