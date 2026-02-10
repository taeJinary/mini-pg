package com.example.minipg.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PgInquiryCacheIntegrationTest {

    @Autowired
    private PgInquiryCache cache;

    @Test
    void cacheHitReturnsValue() {
        String orderId = "cache-order-1";
        cache.evict(orderId);

        cache.put(orderId, new PgInquiryResult(PgInquiryResult.Status.APPROVED));
        PgInquiryResult result = cache.get(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PgInquiryResult.Status.APPROVED);
    }

    @Test
    void notFoundIsCachedBriefly() throws Exception {
        String orderId = "cache-order-2";
        cache.evict(orderId);

        cache.put(orderId, new PgInquiryResult(PgInquiryResult.Status.NOT_FOUND));
        PgInquiryResult result = cache.get(orderId);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PgInquiryResult.Status.NOT_FOUND);

        Thread.sleep(2100);
        PgInquiryResult expired = cache.get(orderId);
        assertThat(expired).isNull();
    }

    @Test
    void evictRemovesValue() {
        String orderId = "cache-order-3";
        cache.evict(orderId);

        cache.put(orderId, new PgInquiryResult(PgInquiryResult.Status.DECLINED));
        cache.evict(orderId);

        PgInquiryResult result = cache.get(orderId);
        assertThat(result).isNull();
    }
}
