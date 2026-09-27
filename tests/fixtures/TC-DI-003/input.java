package com.example.di;

import org.springframework.stereotype.Service;

@Service
class Di03BeanA {
    private final Di03BeanB b;

    public Di03BeanA(Di03BeanB b) {
        this.b = b;
    }
}

@Service
class Di03BeanB {
    private final Di03BeanA a;

    public Di03BeanB(Di03BeanA a) {
        this.a = a;
    }
}