package com.example.minipg.api.dto;

import java.time.LocalDate;

import com.example.minipg.domain.SettlementStatus;

public class AdminSettlementItemResponse {

    private final String settlementId;
    private final String merchantId;
    private final LocalDate settlementDate;
    private final long grossAmount;
    private final long feeAmount;
    private final long netAmount;
    private final SettlementStatus status;

    public AdminSettlementItemResponse(
        String settlementId,
        String merchantId,
        LocalDate settlementDate,
        long grossAmount,
        long feeAmount,
        long netAmount,
        SettlementStatus status
    ) {
        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.settlementDate = settlementDate;
        this.grossAmount = grossAmount;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
        this.status = status;
    }

    public String getSettlementId() {
        return settlementId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public long getGrossAmount() {
        return grossAmount;
    }

    public long getFeeAmount() {
        return feeAmount;
    }

    public long getNetAmount() {
        return netAmount;
    }

    public SettlementStatus getStatus() {
        return status;
    }
}
