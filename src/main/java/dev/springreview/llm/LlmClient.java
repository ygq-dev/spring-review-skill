package dev.springreview.llm;

public interface LlmClient {

    LlmResponse complete(LlmRequest request);

    LlmHealth health();

    boolean enabled();

    String provider();
}
