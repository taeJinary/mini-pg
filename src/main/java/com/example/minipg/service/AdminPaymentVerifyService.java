package com.example.minipg.service;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.minipg.common.ApiException;
import com.example.minipg.common.ErrorCode;
import com.example.minipg.domain.Payment;
import com.example.minipg.domain.PaymentStatus;
import com.example.minipg.domain.repo.PaymentRepository;

@Service
public class AdminPaymentVerifyService {

    private static final Logger log = LoggerFactory.getLogger(AdminPaymentVerifyService.class);
    private static final Duration NOT_FOUND_GRACE = Duration.ofMinutes(2);

    private final PaymentRepository paymentRepository;
    private final FakePgClient fakePgClient;
    private final PgInquiryCache pgInquiryCache;
    private final TransactionTemplate transactionTemplate;

    public AdminPaymentVerifyService(
        PaymentRepository paymentRepository,
        FakePgClient fakePgClient,
        PgInquiryCache pgInquiryCache,
        TransactionTemplate transactionTemplate
    ) {
        this.paymentRepository = paymentRepository;
        this.fakePgClient = fakePgClient;
        this.pgInquiryCache = pgInquiryCache;
        this.transactionTemplate = transactionTemplate;
    }

    public Payment verify(String paymentId) {
        PreparedVerification prepared = transactionTemplate.execute(status -> prepareTx(paymentId));
        PgInquiryResult cached = pgInquiryCache.get(prepared.orderId());
        PgInquiryResult result = cached;
        if (result == null) {
            result = fakePgClient.inquire(prepared.orderId());
            pgInquiryCache.put(prepared.orderId(), result);
        }
        PgInquiryResult finalResult = result;
        return transactionTemplate.execute(status -> applyTx(prepared.paymentId(), finalResult));
    }

    @Transactional
    public PreparedVerification prepareTx(String paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Payment not found"));

        payment.recordVerify(Instant.now());
        paymentRepository.save(payment);

        return new PreparedVerification(
            payment.getId(),
            payment.getOrder().getId(),
            payment.getRequestedAt()
        );
    }

    @Transactional
    public Payment applyTx(String paymentId, PgInquiryResult result) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Payment not found"));

        if (payment.getStatus() != PaymentStatus.REQUESTED) {
            return paymentRepository.save(payment);
        }

        switch (result.getStatus()) {
            case APPROVED:
                payment.markApproved(Instant.now());
                pgInquiryCache.evict(payment.getOrder().getId());
                break;
            case DECLINED:
                payment.markDeclined("PG_DECLINED", "Declined by PG");
                pgInquiryCache.evict(payment.getOrder().getId());
                break;
            case PENDING:
                break;
            case NOT_FOUND:
                handleNotFound(payment, payment.getRequestedAt());
                if (payment.getStatus() == PaymentStatus.DECLINED) {
                    pgInquiryCache.evict(payment.getOrder().getId());
                }
                break;
            default:
                break;
        }

        return paymentRepository.save(payment);
    }

    private void handleNotFound(Payment payment, Instant requestedAt) {
        if (requestedAt == null) {
            log.warn("Payment requestedAt is null for paymentId={}", payment.getId());
            return;
        }
        Instant cutoff = requestedAt.plus(NOT_FOUND_GRACE);
        if (Instant.now().isAfter(cutoff)) {
            payment.markDeclined("PG_NOT_FOUND_TIMEOUT", "PG not found after grace period");
        }
    }

    public record PreparedVerification(
        String paymentId,
        String orderId,
        Instant requestedAt
    ) {
    }
}
