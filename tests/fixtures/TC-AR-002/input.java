package com.example.order.domain;

import com.example.order.infrastructure.persistence.JdbcOrderRepository;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderDomainService {
    private final JdbcOrderRepository repository;

    public OrderDomainService(JdbcOrderRepository repository) {
        this.repository = repository;
    }

    public Object load(Long id) {
        return repository.findById(id);
    }
}