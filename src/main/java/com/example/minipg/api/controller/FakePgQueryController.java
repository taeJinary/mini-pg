package com.example.minipg.api.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fake-pg")
public class FakePgQueryController {

    static final Map<String, PgPaymentStatus> STORE = new ConcurrentHashMap<>();

    @GetMapping("/payments")
    public ResponseEntity<?> query(@RequestParam String orderId) {
        PgPaymentStatus status = STORE.get(orderId);
        if (status == null) {
            return ResponseEntity.ok(new PgQueryResponse("NOT_FOUND"));
        }
        return ResponseEntity.ok(new PgQueryResponse(status.name()));
    }

    public static void record(String orderId, PgPaymentStatus status) {
        STORE.put(orderId, status);
    }

    public enum PgPaymentStatus {
        APPROVED,
        DECLINED,
        PENDING
    }

    private record PgQueryResponse(String status) {
    }
}
