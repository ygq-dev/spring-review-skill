package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di02NegativeService {
    private Dependency dependency;

    public Di02NegativeService(Dependency dependency) {
        this.dependency = dependency;
    }

    static class Dependency {}
}