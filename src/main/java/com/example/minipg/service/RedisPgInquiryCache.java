package com.example.minipg.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPgInquiryCache implements PgInquiryCache {

    private static final String KEY_PREFIX = "pg:inquiry:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttlApproved;
    private final Duration ttlDeclined;
    private final Duration ttlPending;
    private final Duration ttlNotFound;

    public RedisPgInquiryCache(
        StringRedisTemplate redisTemplate,
        @Value("${pg.cache.ttl.approved-seconds:60}") long ttlApprovedSeconds,
        @Value("${pg.cache.ttl.declined-seconds:60}") long ttlDeclinedSeconds,
        @Value("${pg.cache.ttl.pending-seconds:10}") long ttlPendingSeconds,
        @Value("${pg.cache.ttl.not-found-seconds:10}") long ttlNotFoundSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.ttlApproved = Duration.ofSeconds(ttlApprovedSeconds);
        this.ttlDeclined = Duration.ofSeconds(ttlDeclinedSeconds);
        this.ttlPending = Duration.ofSeconds(ttlPendingSeconds);
        this.ttlNotFound = Duration.ofSeconds(ttlNotFoundSeconds);
    }

    @Override
    public PgInquiryResult get(String orderId) {
        String value = redisTemplate.opsForValue().get(key(orderId));
        if (value == null) {
            return null;
        }
        try {
            return new PgInquiryResult(PgInquiryResult.Status.valueOf(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public void put(String orderId, PgInquiryResult result) {
        if (result == null || result.getStatus() == null) {
            return;
        }
        Duration ttl = ttlFor(result.getStatus());
        redisTemplate.opsForValue().set(key(orderId), result.getStatus().name(), ttl);
    }

    @Override
    public void evict(String orderId) {
        redisTemplate.delete(key(orderId));
    }

    private String key(String orderId) {
        return KEY_PREFIX + orderId;
    }

    private Duration ttlFor(PgInquiryResult.Status status) {
        switch (status) {
            case APPROVED:
                return ttlApproved;
            case DECLINED:
                return ttlDeclined;
            case PENDING:
                return ttlPending;
            case NOT_FOUND:
            default:
                return ttlNotFound;
        }
    }
}
