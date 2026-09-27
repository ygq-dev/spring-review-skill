package com.example.di;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
class Di04NegativeService {
    private final Map<String, String> cache = new HashMap<>();

    public void put(String key, String value) {
        cache.put(key, value);
    }
}