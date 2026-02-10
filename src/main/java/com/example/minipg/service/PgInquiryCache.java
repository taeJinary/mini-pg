package com.example.minipg.service;

public interface PgInquiryCache {
    PgInquiryResult get(String orderId);
    void put(String orderId, PgInquiryResult result);
    void evict(String orderId);
}
