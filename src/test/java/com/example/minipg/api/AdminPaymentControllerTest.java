package com.example.minipg.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.example.minipg.domain.Merchant;
import com.example.minipg.domain.Order;
import com.example.minipg.domain.Payment;
import com.example.minipg.domain.PaymentMethod;
import com.example.minipg.domain.repo.MerchantRepository;
import com.example.minipg.domain.repo.OrderRepository;
import com.example.minipg.domain.repo.PaymentRepository;
import com.example.minipg.service.TestPgClientConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestPgClientConfig.class)
class AdminPaymentControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void cleanDb() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    @Test
    void statusFilterWithPaging() throws Exception {
        Merchant merchant = merchantRepository.save(Merchant.create("admin-m1", "admin1@test.com"));
        createRequestedPayment(merchant, "admin-order-1", 1000L, Instant.parse("2026-02-01T10:00:00Z"));
        createRequestedPayment(merchant, "admin-order-2", 2000L, Instant.parse("2026-02-02T10:00:00Z"));
        createRequestedPayment(merchant, "admin-order-3", 3000L, Instant.parse("2026-02-03T10:00:00Z"));
        createApprovedPayment(merchant, "admin-order-4", 4000L, Instant.parse("2026-02-04T10:00:00Z"));

        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/admin/payments?status=REQUESTED&page=0&size=2",
            String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<?> content = (List<?>) data.get("content");
        Number totalElements = (Number) data.get("totalElements");

        assertThat(content.size()).isEqualTo(2);
        assertThat(totalElements.longValue()).isEqualTo(3L);
    }

    @Test
    void merchantAndDateRangeFilter() throws Exception {
        Merchant merchantA = merchantRepository.save(Merchant.create("admin-m2", "admin2@test.com"));
        Merchant merchantB = merchantRepository.save(Merchant.create("admin-m3", "admin3@test.com"));

        createRequestedPayment(merchantA, "admin-order-5", 1500L, Instant.parse("2026-02-01T00:00:00Z"));
        createRequestedPayment(merchantA, "admin-order-6", 1600L, Instant.parse("2026-02-05T00:00:00Z"));
        createRequestedPayment(merchantB, "admin-order-7", 1700L, Instant.parse("2026-02-03T00:00:00Z"));

        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/admin/payments?merchantId=" + merchantA.getId()
                + "&from=2026-02-04T00:00:00Z&to=2026-02-06T00:00:00Z",
            String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Number totalElements = (Number) data.get("totalElements");

        assertThat(totalElements.longValue()).isEqualTo(1L);
    }

    @Test
    void invalidParamsReturnBadRequest() {
        ResponseEntity<String> sizeResponse = restTemplate.getForEntity(
            "/api/admin/payments?size=101",
            String.class
        );
        assertThat(sizeResponse.getStatusCodeValue()).isEqualTo(400);

        ResponseEntity<String> rangeResponse = restTemplate.getForEntity(
            "/api/admin/payments?from=2026-02-10T00:00:00Z&to=2026-02-01T00:00:00Z",
            String.class
        );
        assertThat(rangeResponse.getStatusCodeValue()).isEqualTo(400);
    }

    private void createRequestedPayment(Merchant merchant, String orderNo, long amount, Instant requestedAt) {
        Order order = orderRepository.save(Order.create(merchant, orderNo, amount));
        Payment payment = Payment.create(merchant, order, "idem-" + orderNo, PaymentMethod.CARD, amount);
        payment.markRequested(requestedAt);
        paymentRepository.save(payment);
    }

    private void createApprovedPayment(Merchant merchant, String orderNo, long amount, Instant requestedAt) {
        Order order = orderRepository.save(Order.create(merchant, orderNo, amount));
        Payment payment = Payment.create(merchant, order, "idem-" + orderNo, PaymentMethod.CARD, amount);
        payment.markRequested(requestedAt);
        payment.markApproved(requestedAt.plusSeconds(60));
        paymentRepository.save(payment);
    }
}
