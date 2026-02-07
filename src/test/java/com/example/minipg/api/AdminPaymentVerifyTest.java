package com.example.minipg.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.example.minipg.service.FakePgStore;
import com.example.minipg.domain.Merchant;
import com.example.minipg.domain.Order;
import com.example.minipg.domain.Payment;
import com.example.minipg.domain.PaymentMethod;
import com.example.minipg.domain.PaymentStatus;
import com.example.minipg.domain.repo.MerchantRepository;
import com.example.minipg.domain.repo.OrderRepository;
import com.example.minipg.domain.repo.PaymentRepository;
import com.example.minipg.service.TestPgClientConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestPgClientConfig.class)
class AdminPaymentVerifyTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FakePgStore fakePgStore;

    @BeforeEach
    void cleanDb() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    @Test
    void approveWhenPgApproved() {
        Payment payment = createRequestedPayment("verify-order-1", Instant.parse("2026-02-06T00:00:00Z"));
        fakePgStore.record(payment.getOrder().getId(), FakePgStore.PgPaymentStatus.APPROVED);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/admin/payments/" + payment.getId() + "/verify",
            null,
            String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(updated.getVerifyCount()).isEqualTo(1);
    }

    @Test
    void declineWhenPgDeclined() {
        Payment payment = createRequestedPayment("verify-order-2", Instant.parse("2026-02-06T00:00:00Z"));
        fakePgStore.record(payment.getOrder().getId(), FakePgStore.PgPaymentStatus.DECLINED);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/admin/payments/" + payment.getId() + "/verify",
            null,
            String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(updated.getFailureCode()).isEqualTo("PG_DECLINED");
    }

    @Test
    void notFoundEarlyKeepsRequested() {
        Instant requestedAt = Instant.now().minusSeconds(30);
        Payment payment = createRequestedPayment("verify-order-3", requestedAt);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/admin/payments/" + payment.getId() + "/verify",
            null,
            String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.REQUESTED);
    }

    @Test
    void notFoundLateDeclines() {
        Instant requestedAt = Instant.now().minusSeconds(200);
        Payment payment = createRequestedPayment("verify-order-4", requestedAt);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/admin/payments/" + payment.getId() + "/verify",
            null,
            String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(updated.getFailureCode()).isEqualTo("PG_NOT_FOUND_TIMEOUT");
    }

    private Payment createRequestedPayment(String orderNo, Instant requestedAt) {
        Merchant merchant = merchantRepository.save(Merchant.create("verify-m-" + orderNo, orderNo + "@test.com"));
        Order order = orderRepository.save(Order.create(merchant, orderNo, 1000L));

        Payment payment = Payment.create(merchant, order, "idem-" + orderNo, PaymentMethod.CARD, 1000L);
        payment.markRequested(requestedAt);
        return paymentRepository.save(payment);
    }
}
