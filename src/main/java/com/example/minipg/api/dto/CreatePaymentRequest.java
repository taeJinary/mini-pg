package com.example.minipg.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.example.minipg.domain.PaymentMethod;

public class CreatePaymentRequest {

    @NotBlank
    private String orderId;

    @NotBlank
    private String merchantId;

    @Min(1)
    private long amount;

    @NotNull
    private PaymentMethod method;

    @NotBlank
    private String idempotencyKey;

    private String pgMode;

    public String getOrderId() {
        return orderId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public long getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getPgMode() {
        return pgMode;
    }
}
