package com.example.minipg.service;

import java.time.Instant;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.minipg.api.dto.AdminPaymentItemResponse;
import com.example.minipg.common.ApiException;
import com.example.minipg.common.ErrorCode;
import com.example.minipg.domain.Payment;
import com.example.minipg.domain.PaymentStatus;
import com.example.minipg.domain.repo.PaymentRepository;

@Service
public class AdminPaymentService {

    private static final int MAX_SIZE = 100;
    private static final Set<PaymentStatus> ALLOWED_STATUSES = Set.of(
        PaymentStatus.REQUESTED,
        PaymentStatus.APPROVED,
        PaymentStatus.DECLINED
    );

    private final PaymentRepository paymentRepository;

    public AdminPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Page<AdminPaymentItemResponse> search(
        String merchantId,
        String status,
        Instant from,
        Instant to,
        int page,
        int size
    ) {
        validate(size, from, to);

        PaymentStatus parsedStatus = parseStatus(status);
        Specification<Payment> spec = Specification.where(null);

        if (merchantId != null && !merchantId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("merchant").get("id"), merchantId));
        }

        if (parsedStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsedStatus));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("requestedAt"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("requestedAt"), to));
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        return paymentRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private void validate(int size, Instant from, Instant to) {
        if (size > MAX_SIZE) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "size must be <= 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "from must be <= to");
        }
    }

    private PaymentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            PaymentStatus parsed = PaymentStatus.valueOf(status);
            if (!ALLOWED_STATUSES.contains(parsed)) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "status must be REQUESTED, APPROVED, or DECLINED");
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "status must be REQUESTED, APPROVED, or DECLINED");
        }
    }

    private AdminPaymentItemResponse toResponse(Payment payment) {
        String orderId = payment.getOrder() != null ? payment.getOrder().getId() : null;
        String merchantId = payment.getMerchant() != null ? payment.getMerchant().getId() : null;

        return new AdminPaymentItemResponse(
            payment.getId(),
            orderId,
            merchantId,
            payment.getAmount(),
            payment.getMethod(),
            payment.getStatus(),
            payment.getPgTransactionId(),
            payment.getFailureCode(),
            payment.getFailureMessage(),
            payment.getRequestedAt()
        );
    }
}
