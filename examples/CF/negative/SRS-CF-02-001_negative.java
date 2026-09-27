package com.example.cf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TimeoutConfig {

    @Value("${app.timeout}")
    private long timeout;

    public long getTimeout() {
        return timeout;
    }
}