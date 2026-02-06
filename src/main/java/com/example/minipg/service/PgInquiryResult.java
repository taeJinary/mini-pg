package com.example.minipg.service;

public class PgInquiryResult {

    public enum Status {
        APPROVED,
        DECLINED,
        PENDING,
        NOT_FOUND
    }

    private final Status status;

    public PgInquiryResult(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
