package com.example.minipg.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.minipg.common.ApiResponse;
import com.example.minipg.service.AdminPaymentVerifyService;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentVerifyController {

    private final AdminPaymentVerifyService verifyService;

    public AdminPaymentVerifyController(AdminPaymentVerifyService verifyService) {
        this.verifyService = verifyService;
    }

    @PostMapping("/{paymentId}/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@PathVariable String paymentId) {
        verifyService.verify(paymentId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
