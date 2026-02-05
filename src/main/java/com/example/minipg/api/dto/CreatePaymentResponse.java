package com.example.minipg.api.dto;

import com.example.minipg.domain.PaymentStatus;

public class CreatePaymentResponse {

    private final String paymentId;
    private final PaymentStatus status;

    public CreatePaymentResponse(String paymentId, PaymentStatus status) {
        this.paymentId = paymentId;
        this.status = status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getStatus() {
        return status;
    }
}
