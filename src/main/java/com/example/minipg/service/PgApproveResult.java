package com.example.minipg.service;

public class PgApproveResult {

    public enum Type {
        SUCCESS,
        DECLINED,
        TIMEOUT,
        ERROR
    }

    private final Type type;
    private final String pgTransactionId;
    private final String failureCode;
    private final String failureMessage;

    private PgApproveResult(Type type, String pgTransactionId, String failureCode, String failureMessage) {
        this.type = type;
        this.pgTransactionId = pgTransactionId;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public static PgApproveResult success(String pgTransactionId) {
        return new PgApproveResult(Type.SUCCESS, pgTransactionId, null, null);
    }

    public static PgApproveResult declined(String code, String message) {
        return new PgApproveResult(Type.DECLINED, null, code, message);
    }

    public static PgApproveResult timeout(String code, String message) {
        return new PgApproveResult(Type.TIMEOUT, null, code, message);
    }

    public static PgApproveResult error(String code, String message) {
        return new PgApproveResult(Type.ERROR, null, code, message);
    }

    public Type getType() {
        return type;
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
}
