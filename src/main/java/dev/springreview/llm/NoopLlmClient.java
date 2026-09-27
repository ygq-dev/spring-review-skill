package dev.springreview.llm;

import java.time.Instant;

/**
 * 关闭 / 无密钥时的空实现。
 * complete 总是返回带 error code 的失败响应；不发起任何网络请求。
 */
public final class NoopLlmClient implements LlmClient {

    private final String provider;
    private final String errorCode;

    public NoopLlmClient(String provider, String errorCode) {
        this.provider = provider == null ? "" : provider;
        this.errorCode = errorCode == null ? LlmError.OFFLINE : errorCode;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String trace = request == null ? null : request.traceId();
        LlmError err = new LlmError(errorCode, "llm disabled: " + errorCode,
            false, null, trace);
        return LlmResponse.error(err, 0);
    }

    @Override
    public LlmHealth health() {
        return new LlmHealth(LlmHealth.OFFLINE, provider, "", 0,
            errorCode, Instant.now(), "");
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public String provider() {
        return provider;
    }
}
