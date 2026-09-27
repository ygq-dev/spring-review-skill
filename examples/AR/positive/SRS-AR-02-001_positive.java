package com.example.order.domain;

public class Order {
    private final Long id;
    private final String status;

    public Order(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    public Long id() {
        return id;
    }

    public String status() {
        return status;
    }
}