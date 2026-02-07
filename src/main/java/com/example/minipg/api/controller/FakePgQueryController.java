package com.example.minipg.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.minipg.service.FakePgStore;

@RestController
@RequestMapping("/fake-pg")
public class FakePgQueryController {

    private final FakePgStore store;

    public FakePgQueryController(FakePgStore store) {
        this.store = store;
    }

    @GetMapping("/payments")
    public ResponseEntity<?> query(@RequestParam String orderId) {
        FakePgStore.PgPaymentStatus status = store.get(orderId);
        if (status == null) {
            return ResponseEntity.ok(new PgQueryResponse("NOT_FOUND"));
        }
        return ResponseEntity.ok(new PgQueryResponse(status.name()));
    }

    private record PgQueryResponse(String status) {
    }
}
