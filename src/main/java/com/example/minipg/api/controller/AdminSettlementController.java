package com.example.minipg.api.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.minipg.api.dto.AdminSettlementCreateRequest;
import com.example.minipg.api.dto.AdminSettlementItemResponse;
import com.example.minipg.common.ApiException;
import com.example.minipg.common.ApiResponse;
import com.example.minipg.common.ErrorCode;
import com.example.minipg.service.AdminSettlementService;

@RestController
@RequestMapping("/api/admin/settlements")
public class AdminSettlementController {

    private final AdminSettlementService settlementService;

    public AdminSettlementController(AdminSettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminSettlementItemResponse>> create(
        @Valid @RequestBody AdminSettlementCreateRequest request
    ) {
        AdminSettlementItemResponse response = settlementService.create(
            request.getMerchantId(),
            request.getSettlementDate()
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminSettlementItemResponse>>> search(
        @RequestParam(required = false) String merchantId,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");

        Page<AdminSettlementItemResponse> result = settlementService.search(
            merchantId,
            fromDate,
            toDate,
            page,
            size
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, field + " must be yyyy-MM-dd");
        }
    }
}
