package com.example.minipg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import com.example.minipg.api.dto.CreatePaymentRequest;
import com.example.minipg.api.dto.CreatePaymentResponse;
import com.example.minipg.domain.Merchant;
import com.example.minipg.domain.Order;
import com.example.minipg.domain.PaymentMethod;
import com.example.minipg.domain.repo.MerchantRepository;
import com.example.minipg.domain.repo.OrderRepository;
import com.example.minipg.domain.repo.PaymentRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestPgClientConfig.class)
class PaymentServiceIdempotencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void sameIdempotencyKeyReturnsSamePayment() {
        Merchant merchant = merchantRepository.save(Merchant.create("m1", "m1@test.com"));
        Order order = orderRepository.save(Order.create(merchant, "order-1", 1000L));

        CreatePaymentRequest request = new CreatePaymentRequestBuilder()
            .orderId(order.getId())
            .merchantId(merchant.getId())
            .amount(1000L)
            .method(PaymentMethod.CARD)
            .idempotencyKey("idem-1")
            .pgMode("success")
            .build();

        CreatePaymentResponse first = paymentService.createPayment(request);
        CreatePaymentResponse second = paymentService.createPayment(request);

        assertThat(first.getPaymentId()).isEqualTo(second.getPaymentId());
        assertThat(paymentRepository.countByOrderId(order.getId())).isEqualTo(1);
    }

    @Test
    void concurrentRequestsCreateSinglePayment() throws Exception {
        Merchant merchant = merchantRepository.save(Merchant.create("m2", "m2@test.com"));
        Order order = orderRepository.save(Order.create(merchant, "order-2", 2000L));

        String idempotencyKey = "idem-" + UUID.randomUUID();

        CreatePaymentRequest request = new CreatePaymentRequestBuilder()
            .orderId(order.getId())
            .merchantId(merchant.getId())
            .amount(2000L)
            .method(PaymentMethod.BANK)
            .idempotencyKey(idempotencyKey)
            .pgMode("success")
            .build();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Callable<CreatePaymentResponse>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                startLatch.await();
                return paymentService.createPayment(request);
            });
        }

        List<Future<CreatePaymentResponse>> futures = new ArrayList<>();
        for (Callable<CreatePaymentResponse> task : tasks) {
            futures.add(executor.submit(task));
        }

        startLatch.countDown();

        List<String> paymentIds = new ArrayList<>();
        for (Future<CreatePaymentResponse> future : futures) {
            paymentIds.add(future.get().getPaymentId());
        }

        executor.shutdown();

        assertThat(paymentIds).allMatch(id -> id.equals(paymentIds.get(0)));
        assertThat(paymentRepository.countByOrderId(order.getId())).isEqualTo(1);
    }

    @Test
    void paymentOrderIdIsUnique() {
        Merchant merchant = merchantRepository.save(Merchant.create("m3", "m3@test.com"));
        Order order = orderRepository.save(Order.create(merchant, "order-3", 3000L));

        var first = com.example.minipg.domain.Payment.create(
            merchant,
            order,
            "idem-unique-1",
            PaymentMethod.CARD,
            3000L
        );
        first.markRequested();
        paymentRepository.saveAndFlush(first);

        var second = com.example.minipg.domain.Payment.create(
            merchant,
            order,
            "idem-unique-2",
            PaymentMethod.CARD,
            3000L
        );
        second.markRequested();

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(second))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void pgSuccessStoresTransactionId() {
        Merchant merchant = merchantRepository.save(Merchant.create("m4", "m4@test.com"));
        Order order = orderRepository.save(Order.create(merchant, "order-4", 4000L));

        CreatePaymentRequest request = new CreatePaymentRequestBuilder()
            .orderId(order.getId())
            .merchantId(merchant.getId())
            .amount(4000L)
            .method(PaymentMethod.CARD)
            .idempotencyKey("idem-pg-success")
            .pgMode("success")
            .build();

        CreatePaymentResponse response = paymentService.createPayment(request);

        var payment = paymentRepository.findById(response.getPaymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(com.example.minipg.domain.PaymentStatus.REQUESTED);
        assertThat(payment.getPgTransactionId()).isNotBlank();
    }

    @Test
    void pgTimeoutKeepsRequestedAndStoresFailureCode() {
        Merchant merchant = merchantRepository.save(Merchant.create("m5", "m5@test.com"));
        Order order = orderRepository.save(Order.create(merchant, "order-5", 5000L));

        CreatePaymentRequest request = new CreatePaymentRequestBuilder()
            .orderId(order.getId())
            .merchantId(merchant.getId())
            .amount(5000L)
            .method(PaymentMethod.CARD)
            .idempotencyKey("idem-pg-timeout")
            .pgMode("timeout")
            .build();

        CreatePaymentResponse response = paymentService.createPayment(request);

        var payment = paymentRepository.findById(response.getPaymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(com.example.minipg.domain.PaymentStatus.REQUESTED);
        assertThat(payment.getFailureCode()).isEqualTo("TIMEOUT");
    }

    private static class CreatePaymentRequestBuilder {
        private String orderId;
        private String merchantId;
        private long amount;
        private PaymentMethod method;
        private String idempotencyKey;
        private String pgMode;

        public CreatePaymentRequestBuilder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public CreatePaymentRequestBuilder merchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public CreatePaymentRequestBuilder amount(long amount) {
            this.amount = amount;
            return this;
        }

        public CreatePaymentRequestBuilder method(PaymentMethod method) {
            this.method = method;
            return this;
        }

        public CreatePaymentRequestBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public CreatePaymentRequestBuilder pgMode(String pgMode) {
            this.pgMode = pgMode;
            return this;
        }

        public CreatePaymentRequest build() {
            CreatePaymentRequest request = new CreatePaymentRequest();
            setField(request, "orderId", orderId);
            setField(request, "merchantId", merchantId);
            setField(request, "amount", amount);
            setField(request, "method", method);
            setField(request, "idempotencyKey", idempotencyKey);
            setField(request, "pgMode", pgMode);
            return request;
        }

        private void setField(CreatePaymentRequest request, String fieldName, Object value) {
            try {
                var field = CreatePaymentRequest.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(request, value);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
