package com.example.di;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
class Di05RequestBean {
}

@Service
class Di05NegativeService {
    private final Di05RequestBean requestBean;

    public Di05NegativeService(Di05RequestBean requestBean) {
        this.requestBean = requestBean;
    }
}