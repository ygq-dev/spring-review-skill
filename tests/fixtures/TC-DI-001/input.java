package com.example.di;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Di01NegativeService {
    @Autowired
    private Dependency dependency;

    static class Dependency {}
}