package com.example.minipg.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "payment_events",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_events_event_id", columnNames = "event_id")
    },
    indexes = {
        @Index(name = "idx_payment_event_pg_tx", columnList = "pg_transaction_id")
    }
)
public class PaymentEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "pg_transaction_id", length = 64)
    private String pgTransactionId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 255)
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public static PaymentEvent ofWebhook(
        String eventId,
        String pgTransactionId,
        String payload,
        Instant receivedAt
    ) {
        PaymentEvent event = new PaymentEvent();
        event.eventId = eventId;
        event.pgTransactionId = pgTransactionId;
        event.type = "PG_WEBHOOK";
        event.payload = payload;
        event.receivedAt = receivedAt;
        return event;
    }
}
