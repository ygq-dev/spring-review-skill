package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di03ServiceA {
    private final Di03ServiceB b;

    public Di03ServiceA(Di03ServiceB b) {
        this.b = b;
    }
}

@Service
class Di03ServiceB {
    private final Di03ServiceC c;

    public Di03ServiceB(Di03ServiceC c) {
        this.c = c;
    }
}

@Service
class Di03ServiceC {
}