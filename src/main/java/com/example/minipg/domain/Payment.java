package com.example.minipg.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_idempotency_key", columnNames = "idempotency_key")
    },
    indexes = {
        @Index(name = "idx_payments_merchant_status_created", columnList = "merchant_id, status, created_at"),
        @Index(name = "idx_payments_pg_transaction_id", columnList = "pg_transaction_id")
    }
)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "pg_transaction_id", length = 64)
    private String pgTransactionId;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", length = 255)
    private String failureMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(nullable = false)
    private long amount;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    public static Payment create(
        Merchant merchant,
        Order order,
        String idempotencyKey,
        PaymentMethod method,
        long amount
    ) {
        Payment payment = new Payment();
        payment.merchant = merchant;
        payment.order = order;
        payment.idempotencyKey = idempotencyKey;
        payment.method = method;
        payment.amount = amount;
        return payment;
    }

    public void markRequested() {
        markRequested(Instant.now());
    }

    public void markRequested(Instant now) {
        if (this.status != PaymentStatus.CREATED) {
            throw new IllegalStateException("Payment status can only be requested from CREATED");
        }
        this.status = PaymentStatus.REQUESTED;
        this.requestedAt = now;
    }

    public void attachPgTransaction(String pgTransactionId) {
        this.pgTransactionId = pgTransactionId;
    }

    public void markDeclined(String code, String message) {
        this.status = PaymentStatus.DECLINED;
        this.failureCode = code;
        this.failureMessage = message;
    }

    public void recordFailure(String code, String message) {
        this.failureCode = code;
        this.failureMessage = message;
    }
}
