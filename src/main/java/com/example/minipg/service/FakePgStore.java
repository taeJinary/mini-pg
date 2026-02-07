package com.example.minipg.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class FakePgStore {

    private final Map<String, PgPaymentStatus> store = new ConcurrentHashMap<>();

    public void record(String orderId, PgPaymentStatus status) {
        store.put(orderId, status);
    }

    public PgPaymentStatus get(String orderId) {
        return store.get(orderId);
    }

    public enum PgPaymentStatus {
        APPROVED,
        DECLINED,
        PENDING
    }
}
