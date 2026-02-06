package com.example.minipg.api.dto;

import java.time.Instant;

import com.example.minipg.domain.PaymentMethod;
import com.example.minipg.domain.PaymentStatus;

public class AdminPaymentItemResponse {

    private final String paymentId;
    private final String orderId;
    private final String merchantId;
    private final long amount;
    private final PaymentMethod method;
    private final PaymentStatus status;
    private final String pgTransactionId;
    private final String failureCode;
    private final String failureMessage;
    private final Instant requestedAt;

    public AdminPaymentItemResponse(
        String paymentId,
        String orderId,
        String merchantId,
        long amount,
        PaymentMethod method,
        PaymentStatus status,
        String pgTransactionId,
        String failureCode,
        String failureMessage,
        Instant requestedAt
    ) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.pgTransactionId = pgTransactionId;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.requestedAt = requestedAt;
    }

    public String getPaymentId() {
        return paymentId;
    }

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

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPgTransactionId() {
        return pgTransactionId;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
