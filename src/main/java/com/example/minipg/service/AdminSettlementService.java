package com.example.minipg.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.minipg.api.dto.AdminSettlementItemResponse;
import com.example.minipg.common.ApiException;
import com.example.minipg.common.ErrorCode;
import com.example.minipg.domain.Merchant;
import com.example.minipg.domain.Settlement;
import com.example.minipg.domain.repo.MerchantRepository;
import com.example.minipg.domain.repo.PaymentRepository;
import com.example.minipg.domain.repo.SettlementRepository;

@Service
public class AdminSettlementService {

    private static final int MAX_SIZE = 100;

    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;

    public AdminSettlementService(
        MerchantRepository merchantRepository,
        PaymentRepository paymentRepository,
        SettlementRepository settlementRepository
    ) {
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
    }

    public AdminSettlementItemResponse create(String merchantId, LocalDate settlementDate) {
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Merchant not found"));

        Settlement created = buildSettlement(merchant, settlementDate);
        try {
            Settlement saved = settlementRepository.save(created);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            Settlement existing = settlementRepository
                .findByMerchantIdAndSettlementDate(merchantId, settlementDate)
                .orElseThrow(() -> ex);
            return toResponse(existing);
        }
    }

    public Page<AdminSettlementItemResponse> search(
        String merchantId,
        LocalDate from,
        LocalDate to,
        int page,
        int size
    ) {
        validate(size, from, to);

        Specification<Settlement> spec = Specification.where(null);
        if (merchantId != null && !merchantId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("merchant").get("id"), merchantId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("settlementDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("settlementDate"), to));
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "settlementDate"));
        return settlementRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private Settlement buildSettlement(Merchant merchant, LocalDate settlementDate) {
        Instant from = settlementDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = settlementDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long gross = paymentRepository.sumApprovedAmountByMerchantAndRequestedAtBetween(
            merchant.getId(),
            from,
            to
        );
        long fee = (gross * 25) / 1000;
        long net = gross - fee;
        return Settlement.create(merchant, settlementDate, gross, fee, net);
    }

    private AdminSettlementItemResponse toResponse(Settlement settlement) {
        return new AdminSettlementItemResponse(
            settlement.getId(),
            settlement.getMerchant().getId(),
            settlement.getSettlementDate(),
            settlement.getGrossAmount(),
            settlement.getFeeAmount(),
            settlement.getNetAmount(),
            settlement.getStatus()
        );
    }

    private void validate(int size, LocalDate from, LocalDate to) {
        if (size > MAX_SIZE) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "size must be <= 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "from must be <= to");
        }
    }
}
