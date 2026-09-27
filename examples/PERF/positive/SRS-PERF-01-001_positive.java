package com.example.perf;

import org.springframework.web.client.RestTemplate;

public class RemoteCallBatch {
    private final RestTemplate restTemplate = new RestTemplate();

    public void fetchAll(String[] ids) {
        String idsParam = String.join(",", ids);
        restTemplate.getForObject("https://example.com/api/items?ids=" + idsParam, String.class);
    }
}