package dev.springreview.engine;

import dev.springreview.parser.JavaSourceParser;
import dev.springreview.parser.ParsedBundle;
import dev.springreview.rules.Rule;
import dev.springreview.scope.ReviewScope;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import dev.springreview.tools.IssueCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AST 精准检测器单测。每条规则一个用例；
 * 输入为内联 Java 源码，不落盘、不用 Git。
 */
class AstPreciseDetectorTest {

    // ---------- 通用脚手架 ----------

    private static SourceBundle bundleOf(String path, String content) {
        SourceUnit u = new SourceUnit(path, "java", content,
            "sha256:test", (int) content.lines().count(), "_root", List.of());
        return new SourceBundle("/r", ReviewScope.Mode.FILES,
            List.of(u), null,
            new SourceBundle.Stats(1, (int) content.lines().count(), 0, 0, 0));
    }

    private static ParsedBundle parse(SourceBundle bundle) {
        return new JavaSourceParser().parseAll(bundle);
    }

    private static Rule rule(String id, String l1, String l2) {
        return new Rule(
            id, id, "desc", l1, l2,
            List.of(), "ACTIVE", "MAJOR", "HIGH", "AST",
            "CUSTOM", null, Map.of(), true,
            new Rule.AppliesTo("17+", "3.x",
                List.of("maven"), List.of("all"),
                List.of(), List.of()),
            "message", "remediation", List.of(),
            "1.0.0", "1.0.0", "1.0.0", "team", List.of());
    }

    private static List<IssueCandidate> run(Rule r, SourceBundle bundle) {
        return new AstPreciseDetector().detect(r, bundle, parse(bundle));
    }

    // ---------- SRS-DI-01-001 字段注入 ----------

    @Test
    void di01DetectsAutowiredField() {
        String code = """
                package com.example;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.stereotype.Service;

                @Service
                public class Demo {
                    @Autowired
                    private Dependency dep;
                }
                """;
        Rule r = rule("SRS-DI-01-001", "DI", "01");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).isNotEmpty();
        assertThat(hits).anyMatch(c -> c.evidence().contains("dep"));
    }

    @Test
    void di01DoesNotHitConstructorInjection() {
        String code = """
                package com.example;
                import org.springframework.stereotype.Service;

                @Service
                public class Demo {
                    private final Dependency dep;

                    public Demo(Dependency dep) {
                        this.dep = dep;
                    }
                }
                """;
        Rule r = rule("SRS-DI-01-001", "DI", "01");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-DI-02-001 非 final 依赖 ----------

    @Test
    void di02DetectsNonFinalInjectedField() {
        String code = """
                package com.example;
                import org.springframework.beans.factory.annotation.Autowired;

                public class Demo {
                    @Autowired
                    private Dependency dep;
                }
                """;
        Rule r = rule("SRS-DI-02-001", "DI", "02");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).isNotEmpty();
        assertThat(hits).anyMatch(c -> c.evidence().contains("non-final"));
    }

    @Test
    void di02DoesNotHitFinalField() {
        String code = """
                package com.example;
                import org.springframework.beans.factory.annotation.Autowired;

                public class Demo {
                    @Autowired
                    private final Dependency dep = null;
                }
                """;
        Rule r = rule("SRS-DI-02-001", "DI", "02");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-DAO-01-001 @Transactional private/final ----------

    @Test
    void dao01DetectsPrivateTransactional() {
        String code = """
                package com.example;
                import org.springframework.transaction.annotation.Transactional;

                public class Demo {
                    @Transactional
                    private void bad() { }
                }
                """;
        Rule r = rule("SRS-DAO-01-001", "DAO", "01");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("private").contains("bad");
    }

    @Test
    void dao01DoesNotHitPublicTransactional() {
        String code = """
                package com.example;
                import org.springframework.transaction.annotation.Transactional;

                public class Demo {
                    @Transactional
                    public void ok() { }
                }
                """;
        Rule r = rule("SRS-DAO-01-001", "DAO", "01");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-DAO-02-001 同类自调用 ----------

    @Test
    void dao02DetectsSelfInvocation() {
        String code = """
                package com.example;
                import org.springframework.transaction.annotation.Transactional;

                public class Demo {
                    public void outer() {
                        this.inner();
                    }

                    @Transactional
                    public void inner() { }
                }
                """;
        Rule r = rule("SRS-DAO-02-001", "DAO", "02");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).isNotEmpty();
        assertThat(hits).anyMatch(c -> c.evidence().contains("inner"));
    }

    @Test
    void dao02DoesNotHitDifferentObjectCall() {
        String code = """
                package com.example;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.transaction.annotation.Transactional;

                public class Demo {
                    @Autowired
                    private Other other;

                    public void outer() {
                        other.inner();
                    }

                    @Transactional
                    public void inner() { }
                }
                """;
        Rule r = rule("SRS-DAO-02-001", "DAO", "02");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-DAO-03-001 只读查询未设 readOnly ----------

    @Test
    void dao03DetectsMissingReadOnly() {
        String code = """
                package com.example;
                import org.springframework.transaction.annotation.Transactional;

                public class Demo {
                    @Transactional
                    public String findUser(long id) {
                        return "x";
                    }
                }
                """;
        Rule r = rule("SRS-DAO-03-001", "DAO", "03");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("findUser");
    }

    @Test
    void dao03DoesNotHitWhenReadOnlyTrue() {
        String code = """
                package com.example;
                import org.springframework.transaction.annotation.Transactional;

                public class Demo {
                    @Transactional(readOnly = true)
                    public String findUser(long id) {
                        return "x";
                    }
                }
                """;
        Rule r = rule("SRS-DAO-03-001", "DAO", "03");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    @Test
    void dao03DoesNotHitWriteMethod() {
        String code = """
                package com.example;
                import org.springframework.transaction.annotation.Transactional;

                public class Demo {
                    @Transactional
                    public void save(String s) { }
                }
                """;
        Rule r = rule("SRS-DAO-03-001", "DAO", "03");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-WEB-01-001 Controller 可变字段 ----------

    @Test
    void web01DetectsMutableFieldInController() {
        String code = """
                package com.example;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class Demo {
                    private String state;
                }
                """;
        Rule r = rule("SRS-WEB-01-001", "WEB", "01");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("state");
    }

    @Test
    void web01DoesNotHitFinalField() {
        String code = """
                package com.example;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class Demo {
                    private final Dependency dep = null;
                }
                """;
        Rule r = rule("SRS-WEB-01-001", "WEB", "01");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    @Test
    void web01DoesNotHitNonController() {
        String code = """
                package com.example;
                import org.springframework.stereotype.Service;

                @Service
                public class Demo {
                    private String state;
                }
                """;
        Rule r = rule("SRS-WEB-01-001", "WEB", "01");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-WEB-02-001 @RequestBody 缺 @Valid ----------

    @Test
    void web02DetectsMissingValid() {
        String code = """
                package com.example;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class Demo {
                    @PostMapping("/x")
                    public String create(@RequestBody User user) {
                        return "ok";
                    }
                }
                """;
        Rule r = rule("SRS-WEB-02-001", "WEB", "02");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("user").contains("@Valid");
    }

    @Test
    void web02DoesNotHitWhenValidPresent() {
        String code = """
                package com.example;
                import jakarta.validation.Valid;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class Demo {
                    @PostMapping("/x")
                    public String create(@Valid @RequestBody User user) {
                        return "ok";
                    }
                }
                """;
        Rule r = rule("SRS-WEB-02-001", "WEB", "02");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- 未知规则不响应 ----------

    @Test
    void unsupportedRuleNotHandled() {
        Rule r = rule("SRS-XXX-99-999", "XXX", "99");
        assertThat(new AstPreciseDetector().supports(r)).isFalse();
    }

    // ---------- SRS-CON-01-001 静态共享 SimpleDateFormat ----------

    @Test
    void con01DetectsStaticSimpleDateFormat() {
        String code = """
                package com.example;
                import java.text.SimpleDateFormat;

                public class DateUtil {
                    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
                }
                """;
        Rule r = rule("SRS-CON-01-001", "CON", "01");
        List<IssueCandidate> hits = run(r, bundleOf("src/DateUtil.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("SDF");
    }

    @Test
    void con01DoesNotHitLocalVariable() {
        String code = """
                package com.example;
                import java.text.SimpleDateFormat;

                public class DateUtil {
                    public String format() {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        return sdf.format(new java.util.Date());
                    }
                }
                """;
        Rule r = rule("SRS-CON-01-001", "CON", "01");
        assertThat(run(r, bundleOf("src/DateUtil.java", code))).isEmpty();
    }

    @Test
    void con01DoesNotHitDateTimeFormatter() {
        String code = """
                package com.example;
                import java.time.format.DateTimeFormatter;

                public class DateUtil {
                    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                }
                """;
        Rule r = rule("SRS-CON-01-001", "CON", "01");
        assertThat(run(r, bundleOf("src/DateUtil.java", code))).isEmpty();
    }

    // ---------- SRS-SEC-03-001 SQL 拼接 ----------

    @Test
    void sec03DetectsSqlConcatenation() {
        String code = """
                package com.example;

                public class Demo {
                    public String find(String name) {
                        return "SELECT * FROM users WHERE name = '" + name + "'";
                    }
                }
                """;
        Rule r = rule("SRS-SEC-03-001", "SEC", "03");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).isNotEmpty();
        assertThat(hits).anyMatch(c -> c.evidence().contains("SELECT"));
    }

    @Test
    void sec03DoesNotHitParameterizedSql() {
        String code = """
                package com.example;

                public class Demo {
                    public String find() {
                        return "SELECT * FROM users WHERE name = ?";
                    }
                }
                """;
        Rule r = rule("SRS-SEC-03-001", "SEC", "03");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    @Test
    void sec03DoesNotHitUnrelatedConcat() {
        String code = """
                package com.example;

                public class Demo {
                    public String greet(String name) {
                        return "Hello, " + name;
                    }
                }
                """;
        Rule r = rule("SRS-SEC-03-001", "SEC", "03");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-EXC-01-001 catch 吞异常 ----------

    @Test
    void exc01DetectsEmptyCatch() {
        String code = """
                package com.example;

                public class Demo {
                    public void run() {
                        try {
                            doWork();
                        } catch (Exception e) {
                        }
                    }
                    private void doWork() { }
                }
                """;
        Rule r = rule("SRS-EXC-01-001", "EXC", "01");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("Exception");
    }

    @Test
    void exc01DoesNotHitCatchWithLog() {
        String code = """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;

                public class Demo {
                    private static final Logger log = LoggerFactory.getLogger(Demo.class);

                    public void run() {
                        try {
                            doWork();
                        } catch (Exception e) {
                            log.error("fail", e);
                        }
                    }
                    private void doWork() { }
                }
                """;
        Rule r = rule("SRS-EXC-01-001", "EXC", "01");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    @Test
    void exc01DoesNotHitCatchWithRethrow() {
        String code = """
                package com.example;

                public class Demo {
                    public void run() {
                        try {
                            doWork();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    private void doWork() { }
                }
                """;
        Rule r = rule("SRS-EXC-01-001", "EXC", "01");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }
    // ---------- SRS-DAO-04-001 事务内远程调用 ----------

    @Test
    void dao04DetectsRemoteInTransaction() {
        String code = """
                package com.example;
                import org.springframework.transaction.annotation.Transactional;
                import org.springframework.web.client.RestTemplate;

                public class Demo {
                    private final RestTemplate restTemplate = new RestTemplate();

                    @Transactional
                    public void process() {
                        restTemplate.getForObject("http://x", String.class);
                    }
                }
                """;
        Rule r = rule("SRS-DAO-04-001", "DAO", "04");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("getForObject");
    }

    @Test
    void dao04DoesNotHitOutsideTransaction() {
        String code = """
                package com.example;
                import org.springframework.web.client.RestTemplate;

                public class Demo {
                    private final RestTemplate restTemplate = new RestTemplate();

                    public void process() {
                        restTemplate.getForObject("http://x", String.class);
                    }
                }
                """;
        Rule r = rule("SRS-DAO-04-001", "DAO", "04");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-WEB-04-001 跨域过宽 ----------

    @Test
    void web04DetectsWildcardWithCredentials() {
        String code = """
                package com.example;
                import org.springframework.web.servlet.config.annotation.CorsRegistry;
                import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class CorsConfig implements WebMvcConfigurer {
                    @Override
                    public void addCorsMappings(CorsRegistry registry) {
                        registry.addMapping("/**")
                            .allowedOrigins("*")
                            .allowCredentials(true);
                    }
                }
                """;
        Rule r = rule("SRS-WEB-04-001", "WEB", "04");
        List<IssueCandidate> hits = run(r, bundleOf("src/CorsConfig.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("allowedOrigins");
    }

    @Test
    void web04DoesNotHitWhenNoCredentials() {
        String code = """
                package com.example;
                import org.springframework.web.servlet.config.annotation.CorsRegistry;
                import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class CorsConfig implements WebMvcConfigurer {
                    @Override
                    public void addCorsMappings(CorsRegistry registry) {
                        registry.addMapping("/**").allowedOrigins("*");
                    }
                }
                """;
        Rule r = rule("SRS-WEB-04-001", "WEB", "04");
        assertThat(run(r, bundleOf("src/CorsConfig.java", code))).isEmpty();
    }

    // ---------- SRS-SEC-04-001 关闭 CSRF ----------

    @Test
    void sec04DetectsCsrfDisable() {
        String code = """
                package com.example;
                import org.springframework.security.config.annotation.web.builders.HttpSecurity;

                public class Demo {
                    public void config(HttpSecurity http) throws Exception {
                        http.csrf(csrf -> csrf.disable());
                    }
                }
                """;
        Rule r = rule("SRS-SEC-04-001", "SEC", "04");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).isNotEmpty();
        assertThat(hits).anyMatch(c -> c.evidence().contains("csrf"));
    }

    @Test
    void sec04DoesNotHitCsrfEnabled() {
        String code = """
                package com.example;
                import org.springframework.security.config.annotation.web.builders.HttpSecurity;

                public class Demo {
                    public void config(HttpSecurity http) throws Exception {
                        http.csrf(csrf -> csrf.csrfTokenRepository(null));
                    }
                }
                """;
        Rule r = rule("SRS-SEC-04-001", "SEC", "04");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-RES-01-001 未关闭资源 ----------

    @Test
    void res01DetectsUnclosedInputStream() {
        String code = """
                package com.example;
                import java.io.FileInputStream;
                import java.io.InputStream;

                public class Demo {
                    public void read() throws Exception {
                        InputStream in = new FileInputStream("data.txt");
                        in.read();
                    }
                }
                """;
        Rule r = rule("SRS-RES-01-001", "RES", "01");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("in").contains("InputStream");
    }

    @Test
    void res01DoesNotHitTryWithResources() {
        String code = """
                package com.example;
                import java.io.FileInputStream;
                import java.io.InputStream;

                public class Demo {
                    public void read() throws Exception {
                        try (InputStream in = new FileInputStream("data.txt")) {
                            in.read();
                        }
                    }
                }
                """;
        Rule r = rule("SRS-RES-01-001", "RES", "01");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    @Test
    void res01DoesNotHitExplicitClose() {
        String code = """
                package com.example;
                import java.io.FileInputStream;
                import java.io.InputStream;

                public class Demo {
                    public void read() throws Exception {
                        InputStream in = new FileInputStream("data.txt");
                        try { in.read(); } finally { in.close(); }
                    }
                }
                """;
        Rule r = rule("SRS-RES-01-001", "RES", "01");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }

    // ---------- SRS-OBS-01-001 日志拼接 ----------

    @Test
    void obs01DetectsLogConcat() {
        String code = """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;

                public class Demo {
                    private static final Logger log = LoggerFactory.getLogger(Demo.class);

                    public void run(String user) {
                        log.info("user=" + user);
                    }
                }
                """;
        Rule r = rule("SRS-OBS-01-001", "OBS", "01");
        List<IssueCandidate> hits = run(r, bundleOf("src/Demo.java", code));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).evidence()).contains("log concat");
    }

    @Test
    void obs01DoesNotHitPlaceholder() {
        String code = """
                package com.example;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;

                public class Demo {
                    private static final Logger log = LoggerFactory.getLogger(Demo.class);

                    public void run(String user) {
                        log.info("user={}", user);
                    }
                }
                """;
        Rule r = rule("SRS-OBS-01-001", "OBS", "01");
        assertThat(run(r, bundleOf("src/Demo.java", code))).isEmpty();
    }
}
