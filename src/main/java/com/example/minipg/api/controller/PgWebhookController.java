package com.example.minipg.api.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.minipg.api.dto.PgWebhookRequest;
import com.example.minipg.common.ApiResponse;
import com.example.minipg.service.PgWebhookService;

@RestController
@RequestMapping("/api/pg")
public class PgWebhookController {

    private final PgWebhookService pgWebhookService;

    public PgWebhookController(PgWebhookService pgWebhookService) {
        this.pgWebhookService = pgWebhookService;
    }

    @PostMapping("/webhooks")
    public ResponseEntity<ApiResponse<Void>> receiveWebhook(@Valid @RequestBody PgWebhookRequest request) {
        pgWebhookService.handle(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
