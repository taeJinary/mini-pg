package com.example.minipg.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.example.minipg.api.dto.AdminSettlementCreateRequest;
import com.example.minipg.domain.Merchant;
import com.example.minipg.domain.Order;
import com.example.minipg.domain.Payment;
import com.example.minipg.domain.PaymentMethod;
import com.example.minipg.domain.PaymentStatus;
import com.example.minipg.domain.repo.MerchantRepository;
import com.example.minipg.domain.repo.OrderRepository;
import com.example.minipg.domain.repo.PaymentRepository;
import com.example.minipg.domain.repo.SettlementRepository;
import com.example.minipg.service.TestPgClientConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestPgClientConfig.class)
class AdminSettlementControllerTest {

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

    @Autowired
    private SettlementRepository settlementRepository;

    @BeforeEach
    void cleanDb() {
        settlementRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    @Test
    void approvedPaymentsAreAggregated() throws Exception {
        Merchant merchant = merchantRepository.save(Merchant.create("settle-m1", "m1@test.com"));
        LocalDate date = LocalDate.parse("2026-02-01");
        Instant inRange1 = date.atStartOfDay(ZoneOffset.UTC).plusHours(1).toInstant();
        Instant inRange2 = date.atStartOfDay(ZoneOffset.UTC).plusHours(5).toInstant();

        createApprovedPayment(merchant, "s-order-1", 1000L, inRange1);
        createApprovedPayment(merchant, "s-order-2", 3000L, inRange2);

        AdminSettlementCreateRequest request = new AdminSettlementCreateRequestBuilder()
            .merchantId(merchant.getId())
            .settlementDate(date)
            .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/admin/settlements",
            request,
            String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");

        Number gross = (Number) data.get("grossAmount");
        Number fee = (Number) data.get("feeAmount");
        Number net = (Number) data.get("netAmount");

        assertThat(gross.longValue()).isEqualTo(4000L);
        assertThat(fee.longValue()).isEqualTo(100L);
        assertThat(net.longValue()).isEqualTo(3900L);
    }

    @Test
    void excludesOtherDatesAndMerchants() throws Exception {
        Merchant merchantA = merchantRepository.save(Merchant.create("settle-m2", "m2@test.com"));
        Merchant merchantB = merchantRepository.save(Merchant.create("settle-m3", "m3@test.com"));
        LocalDate date = LocalDate.parse("2026-02-02");

        Instant inRange = date.atStartOfDay(ZoneOffset.UTC).plusHours(2).toInstant();
        Instant otherDate = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).plusHours(1).toInstant();

        createApprovedPayment(merchantA, "s-order-3", 2000L, inRange);
        createApprovedPayment(merchantA, "s-order-4", 5000L, otherDate);
        createApprovedPayment(merchantB, "s-order-5", 7000L, inRange);

        AdminSettlementCreateRequest request = new AdminSettlementCreateRequestBuilder()
            .merchantId(merchantA.getId())
            .settlementDate(date)
            .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/admin/settlements",
            request,
            String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Number gross = (Number) data.get("grossAmount");

        assertThat(gross.longValue()).isEqualTo(2000L);
    }

    @Test
    void createIsIdempotentByMerchantAndDate() {
        Merchant merchant = merchantRepository.save(Merchant.create("settle-m4", "m4@test.com"));
        LocalDate date = LocalDate.parse("2026-02-03");

        AdminSettlementCreateRequest request = new AdminSettlementCreateRequestBuilder()
            .merchantId(merchant.getId())
            .settlementDate(date)
            .build();

        ResponseEntity<String> first = restTemplate.postForEntity("/api/admin/settlements", request, String.class);
        ResponseEntity<String> second = restTemplate.postForEntity("/api/admin/settlements", request, String.class);

        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(settlementRepository.countByMerchantIdAndSettlementDate(merchant.getId(), date)).isEqualTo(1L);
    }

    @Test
    void searchFiltersAndPaging() throws Exception {
        Merchant merchant = merchantRepository.save(Merchant.create("settle-m5", "m5@test.com"));
        LocalDate date1 = LocalDate.parse("2026-02-04");
        LocalDate date2 = LocalDate.parse("2026-02-05");

        createSettlement(merchant, date1, 1000L);
        createSettlement(merchant, date2, 2000L);

        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/admin/settlements?merchantId=" + merchant.getId() + "&from=2026-02-04&to=2026-02-05&page=0&size=1",
            String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = objectMapper.readValue(response.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<?> content = (List<?>) data.get("content");
        Number totalElements = (Number) data.get("totalElements");

        assertThat(content.size()).isEqualTo(1);
        assertThat(totalElements.longValue()).isEqualTo(2L);
    }

    private void createApprovedPayment(Merchant merchant, String orderNo, long amount, Instant requestedAt) {
        Order order = orderRepository.save(Order.create(merchant, orderNo, amount));
        Payment payment = Payment.create(merchant, order, "idem-" + orderNo, PaymentMethod.CARD, amount);
        payment.markRequested(requestedAt);
        payment.markApproved(requestedAt.plusSeconds(60));
        paymentRepository.save(payment);
    }

    private void createSettlement(Merchant merchant, LocalDate date, long grossAmount) {
        long fee = (grossAmount * 25) / 1000;
        long net = grossAmount - fee;
        settlementRepository.save(com.example.minipg.domain.Settlement.create(merchant, date, grossAmount, fee, net));
    }

    private static class AdminSettlementCreateRequestBuilder {
        private String merchantId;
        private LocalDate settlementDate;

        public AdminSettlementCreateRequestBuilder merchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public AdminSettlementCreateRequestBuilder settlementDate(LocalDate settlementDate) {
            this.settlementDate = settlementDate;
            return this;
        }

        public AdminSettlementCreateRequest build() {
            AdminSettlementCreateRequest request = new AdminSettlementCreateRequest();
            setField(request, "merchantId", merchantId);
            setField(request, "settlementDate", settlementDate);
            return request;
        }

        private void setField(AdminSettlementCreateRequest request, String fieldName, Object value) {
            try {
                var field = AdminSettlementCreateRequest.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(request, value);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
