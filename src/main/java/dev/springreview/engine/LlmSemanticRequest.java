package dev.springreview.engine;

import dev.springreview.rules.Rule;
import dev.springreview.scope.SourceUnit;

import java.util.List;

public record LlmSemanticRequest(
    Rule rule,
    List<SourceUnit> units,
    String traceId
) {
}
