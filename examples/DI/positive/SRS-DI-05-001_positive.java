package com.example.di;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Scope("prototype")
class Di05PrototypeBean {
}

@Service
class Di05PositiveService {
    private final ObjectProvider<Di05PrototypeBean> provider;

    public Di05PositiveService(ObjectProvider<Di05PrototypeBean> provider) {
        this.provider = provider;
    }
}