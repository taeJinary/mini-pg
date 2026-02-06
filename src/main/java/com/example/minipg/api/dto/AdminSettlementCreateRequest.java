package com.example.minipg.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AdminSettlementCreateRequest {

    @NotBlank
    private String merchantId;

    @NotNull
    private LocalDate settlementDate;

    public String getMerchantId() {
        return merchantId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }
}
