package com.example.minipg.api.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.minipg.api.dto.CreatePaymentRequest;
import com.example.minipg.api.dto.CreatePaymentResponse;
import com.example.minipg.common.ApiResponse;
import com.example.minipg.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
        @Valid @RequestBody CreatePaymentRequest request
    ) {
        CreatePaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
