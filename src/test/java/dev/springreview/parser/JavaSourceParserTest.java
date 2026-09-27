package dev.springreview.parser;

import dev.springreview.scope.ReviewScope;
import dev.springreview.scope.SourceBundle;
import dev.springreview.scope.SourceUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceParserTest {

    private final JavaSourceParser parser = new JavaSourceParser();

    private static SourceUnit unit(String path, String content) {
        return new SourceUnit(path, "java", content, "sha256:x", 1, "_root", List.of());
    }

    @Test
    void parsesPackageAndImports() {
        String code = """
                package com.example.order.controller;

                import com.example.order.repository.OrderRepository;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class OrderController {
                    private final OrderRepository repo;
                    public OrderController(OrderRepository repo) { this.repo = repo; }
                }
                """;
        ParsedSource ps = parser.parse(unit("src/main/java/OrderController.java", code));
        assertThat(ps.packageName()).isEqualTo("com.example.order.controller");
        assertThat(ps.imports()).hasSize(2);
        assertThat(ps.types()).hasSize(1);
        assertThat(ps.types().get(0).simpleName()).isEqualTo("OrderController");
        assertThat(ps.types().get(0).annotations()).contains("RestController");
        assertThat(ps.parseErrors()).isEmpty();
    }

    @Test
    void detectsControllerToRepositoryEdge() {
        String code = """
                package com.example.order.controller;

                import com.example.order.repository.OrderRepository;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class OrderController {
                    private final OrderRepository repository;
                    public OrderController(OrderRepository repository) { this.repository = repository; }
                }
                """;
        ParsedSource ps = parser.parse(unit("OrderController.java", code));
        DependencyGraph g = ps.dependencyGraph();
        assertThat(g.classEdges()).isNotEmpty();
        boolean found = g.classEdges().values().stream()
            .flatMap(List::stream)
            .anyMatch(e -> "OrderRepository".equals(e.to())
                && e.kind() == DependencyEdge.Kind.FIELD_TYPE);
        assertThat(found).isTrue();
    }

    @Test
    void detectsDomainToWebEdge() {
        String code = """
                package com.example.order.domain;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class OrderDomainService {
                }
                """;
        ParsedSource ps = parser.parse(unit("OrderDomainService.java", code));
        boolean annoFound = ps.dependencyGraph().classEdges().values().stream()
            .flatMap(List::stream)
            .anyMatch(e -> "RestController".equals(e.to())
                && e.kind() == DependencyEdge.Kind.ANNOTATION);
        assertThat(annoFound).isTrue();
    }

    @Test
    void syntaxErrorIsolated() {
        String bad = "public class Broken { this is not java ";
        ParsedSource ps = parser.parse(unit("Broken.java", bad));
        assertThat(ps.parseErrors()).isNotEmpty();
        assertThat(ps.types()).isEmpty();
    }

    @Test
    void parseAllHandlesMultipleUnits() {
        SourceUnit u1 = unit("A.java", "package p; public class A {}");
        SourceUnit u2 = unit("B.java", "package p; public class B {}");
        SourceBundle bundle = new SourceBundle("/r", ReviewScope.Mode.FILES,
            List.of(u1, u2), null,
            new SourceBundle.Stats(2, 2, 0, 0, 0));
        ParsedBundle parsed = parser.parseAll(bundle);
        assertThat(parsed.parsedSources()).hasSize(2);
        assertThat(parsed.stats().typeCount()).isEqualTo(2);
        assertThat(parsed.mode()).isEqualTo(ParsedBundle.Mode.FULL);
    }

    @Test
    void emptyContentProducesSkipError() {
        ParsedSource ps = parser.parse(unit("Empty.java", ""));
        assertThat(ps.parseErrors()).hasSize(1);
        assertThat(ps.parseErrors().get(0).errorType()).isEqualTo(ParseError.ErrorType.SKIP);
    }
}
