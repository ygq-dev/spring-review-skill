package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di02PositiveService {
    private final Dependency dependency;

    public Di02PositiveService(Dependency dependency) {
        this.dependency = dependency;
    }

    static class Dependency {}
}