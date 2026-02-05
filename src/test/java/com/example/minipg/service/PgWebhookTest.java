package com.example.minipg.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.example.minipg.api.dto.PgWebhookRequest;
import com.example.minipg.domain.Merchant;
import com.example.minipg.domain.Order;
import com.example.minipg.domain.Payment;
import com.example.minipg.domain.PaymentMethod;
import com.example.minipg.domain.PaymentStatus;
import com.example.minipg.domain.repo.MerchantRepository;
import com.example.minipg.domain.repo.OrderRepository;
import com.example.minipg.domain.repo.PaymentEventRepository;
import com.example.minipg.domain.repo.PaymentRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestPgClientConfig.class)
class PgWebhookTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void sameEventIdIsIdempotent() {
        PgWebhookRequest request = new PgWebhookRequestBuilder()
            .eventId("evt-1")
            .pgTransactionId("pg-1")
            .status(PgWebhookRequest.Status.APPROVED)
            .build();

        ResponseEntity<String> first = post(request);
        ResponseEntity<String> second = post(request);

        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(paymentEventRepository.countByEventId("evt-1")).isEqualTo(1);
    }

    @Test
    void requestedToApprovedTransition() {
        Merchant merchant = merchantRepository.save(Merchant.create("m6", "m6@test.com"));
        Order order = orderRepository.save(Order.create(merchant, "order-6", 6000L));

        Payment payment = Payment.create(merchant, order, "idem-webhook-1", PaymentMethod.CARD, 6000L);
        payment.markRequested();
        payment.attachPgTransaction("pg-approve-1");
        paymentRepository.save(payment);

        PgWebhookRequest request = new PgWebhookRequestBuilder()
            .eventId("evt-2")
            .pgTransactionId("pg-approve-1")
            .status(PgWebhookRequest.Status.APPROVED)
            .build();

        ResponseEntity<String> response = post(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void missingPgTransactionIdReturnsOk() {
        PgWebhookRequest request = new PgWebhookRequestBuilder()
            .eventId("evt-3")
            .pgTransactionId("pg-missing-1")
            .status(PgWebhookRequest.Status.DECLINED)
            .failureCode("DECLINED")
            .failureMessage("no tx")
            .build();

        ResponseEntity<String> response = post(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(paymentEventRepository.existsByEventId("evt-3")).isTrue();
    }

    private ResponseEntity<String> post(PgWebhookRequest request) {
        return restTemplate.postForEntity("/api/pg/webhooks", request, String.class);
    }

    private static class PgWebhookRequestBuilder {
        private String eventId;
        private String pgTransactionId;
        private PgWebhookRequest.Status status;
        private String failureCode;
        private String failureMessage;

        public PgWebhookRequestBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public PgWebhookRequestBuilder pgTransactionId(String pgTransactionId) {
            this.pgTransactionId = pgTransactionId;
            return this;
        }

        public PgWebhookRequestBuilder status(PgWebhookRequest.Status status) {
            this.status = status;
            return this;
        }

        public PgWebhookRequestBuilder failureCode(String failureCode) {
            this.failureCode = failureCode;
            return this;
        }

        public PgWebhookRequestBuilder failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        public PgWebhookRequest build() {
            PgWebhookRequest request = new PgWebhookRequest();
            setField(request, "eventId", eventId);
            setField(request, "pgTransactionId", pgTransactionId);
            setField(request, "status", status);
            setField(request, "failureCode", failureCode);
            setField(request, "failureMessage", failureMessage);
            return request;
        }

        private void setField(PgWebhookRequest request, String fieldName, Object value) {
            try {
                var field = PgWebhookRequest.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(request, value);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
