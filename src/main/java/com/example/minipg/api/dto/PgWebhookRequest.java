package com.example.minipg.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PgWebhookRequest {

    public enum Status {
        APPROVED,
        DECLINED
    }

    @NotBlank
    private String eventId;

    @NotBlank
    private String pgTransactionId;

    @NotNull
    private Status status;

    private String failureCode;

    private String failureMessage;

    public String getEventId() {
        return eventId;
    }

    public String getPgTransactionId() {
        return pgTransactionId;
    }

    public Status getStatus() {
        return status;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}
