package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di01PositiveService {
    private final Dependency dependency;

    public Di01PositiveService(Dependency dependency) {
        this.dependency = dependency;
    }

    static class Dependency {}
}