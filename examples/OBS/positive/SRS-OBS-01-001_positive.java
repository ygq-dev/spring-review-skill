package com.example.obs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public void logUser(String userId) {
        log.info("user={}", userId);
    }
}