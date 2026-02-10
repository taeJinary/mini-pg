package com.example.minipg.service;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.minipg.api.dto.PgWebhookRequest;
import com.example.minipg.domain.Payment;
import com.example.minipg.domain.PaymentEvent;
import com.example.minipg.domain.PaymentStatus;
import com.example.minipg.domain.repo.PaymentEventRepository;
import com.example.minipg.domain.repo.PaymentRepository;

@Service
public class PgWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PgWebhookService.class);

    private final PaymentEventRepository paymentEventRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final PgInquiryCache pgInquiryCache;

    public PgWebhookService(
        PaymentEventRepository paymentEventRepository,
        PaymentRepository paymentRepository,
        ObjectMapper objectMapper,
        PgInquiryCache pgInquiryCache
    ) {
        this.paymentEventRepository = paymentEventRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.pgInquiryCache = pgInquiryCache;
    }

    @Transactional
    public void handle(PgWebhookRequest request) {
        if (paymentEventRepository.existsByEventId(request.getEventId())) {
            return;
        }

        String payload = toPayload(request);
        PaymentEvent event = PaymentEvent.ofWebhook(
            request.getEventId(),
            request.getPgTransactionId(),
            payload,
            Instant.now()
        );

        try {
            paymentEventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException ex) {
            if (paymentEventRepository.existsByEventId(request.getEventId())) {
                return;
            }
            throw ex;
        }

        Optional<Payment> paymentOpt = paymentRepository.findByPgTransactionId(request.getPgTransactionId());
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for pgTransactionId={}", request.getPgTransactionId());
            return;
        }

        Payment payment = paymentOpt.get();
        String orderId = payment.getOrder().getId();
        if (payment.getStatus() == PaymentStatus.REQUESTED) {
            if (request.getStatus() == PgWebhookRequest.Status.APPROVED) {
                payment.markApproved(Instant.now());
            } else {
                payment.markDeclined(request.getFailureCode(), request.getFailureMessage());
            }
            paymentRepository.save(payment);
            pgInquiryCache.evict(orderId);
            return;
        }

        if (payment.getStatus() == PaymentStatus.APPROVED && request.getStatus() == PgWebhookRequest.Status.DECLINED) {
            log.warn("Ignoring reverse transition APPROVED -> DECLINED for paymentId={}", payment.getId());
            return;
        }

        if (payment.getStatus() == PaymentStatus.DECLINED && request.getStatus() == PgWebhookRequest.Status.APPROVED) {
            log.warn("Ignoring reverse transition DECLINED -> APPROVED for paymentId={}", payment.getId());
            return;
        }
    }

    private String toPayload(PgWebhookRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
