package com.example.minipg.api.controller;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.minipg.api.dto.AdminPaymentItemResponse;
import com.example.minipg.common.ApiException;
import com.example.minipg.common.ApiResponse;
import com.example.minipg.common.ErrorCode;
import com.example.minipg.service.AdminPaymentService;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminPaymentItemResponse>>> search(
        @RequestParam(required = false) String merchantId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Instant fromInstant = parseInstant(from, "from");
        Instant toInstant = parseInstant(to, "to");

        Page<AdminPaymentItemResponse> result = adminPaymentService.search(
            merchantId,
            status,
            fromInstant,
            toInstant,
            page,
            size
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private Instant parseInstant(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, field + " must be ISO-8601 instant");
        }
    }
}
