package com.example.perf;

import org.springframework.web.client.RestTemplate;

public class RemoteCallInLoop {
    private final RestTemplate restTemplate = new RestTemplate();

    public void fetchAll(String[] ids) {
        for (String id : ids) {
            restTemplate.getForObject("https://example.com/api/items/" + id, String.class);
        }
    }
}