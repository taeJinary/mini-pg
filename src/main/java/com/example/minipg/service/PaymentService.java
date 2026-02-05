package com.example.minipg.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.minipg.api.dto.CreatePaymentRequest;
import com.example.minipg.api.dto.CreatePaymentResponse;
import com.example.minipg.common.ApiException;
import com.example.minipg.common.ErrorCode;
import com.example.minipg.domain.Merchant;
import com.example.minipg.domain.Order;
import com.example.minipg.domain.Payment;
import com.example.minipg.domain.repo.MerchantRepository;
import com.example.minipg.domain.repo.OrderRepository;
import com.example.minipg.domain.repo.PaymentRepository;

@Service
public class PaymentService {

    private final MerchantRepository merchantRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionTemplate transactionTemplate;
    private final FakePgClient fakePgClient;

    public PaymentService(
        MerchantRepository merchantRepository,
        OrderRepository orderRepository,
        PaymentRepository paymentRepository,
        TransactionTemplate transactionTemplate,
        FakePgClient fakePgClient
    ) {
        this.merchantRepository = merchantRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.transactionTemplate = transactionTemplate;
        this.fakePgClient = fakePgClient;
    }

    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return new CreatePaymentResponse(existing.get().getId(), existing.get().getStatus());
        }

        Merchant merchant = merchantRepository.findById(request.getMerchantId())
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Merchant not found"));

        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Order not found"));

        if (!order.getMerchant().getId().equals(merchant.getId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Order does not belong to merchant");
        }

        Payment saved;
        try {
            saved = transactionTemplate.execute(status -> {
                Payment payment = Payment.create(
                    merchant,
                    order,
                    request.getIdempotencyKey(),
                    request.getMethod(),
                    request.getAmount()
                );
                payment.markRequested(Instant.now());
                return paymentRepository.saveAndFlush(payment);
            });
        } catch (DataIntegrityViolationException ex) {
            for (int attempt = 0; attempt < 3; attempt++) {
                Optional<Payment> byIdempotency = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
                if (byIdempotency.isPresent()) {
                    return new CreatePaymentResponse(byIdempotency.get().getId(), byIdempotency.get().getStatus());
                }

                Optional<Payment> byOrder = paymentRepository.findByOrderId(request.getOrderId());
                if (byOrder.isPresent()) {
                    return new CreatePaymentResponse(byOrder.get().getId(), byOrder.get().getStatus());
                }

                if (attempt < 2) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            throw ex;
        }

        String mode = request.getPgMode();
        PgApproveResult result = fakePgClient.approve(mode);

        Payment updated = transactionTemplate.execute(status -> {
            Payment payment = paymentRepository.findById(saved.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL, "Payment not found after create"));

            switch (result.getType()) {
                case SUCCESS:
                    payment.attachPgTransaction(result.getPgTransactionId());
                    break;
                case DECLINED:
                    payment.markDeclined(result.getFailureCode(), result.getFailureMessage());
                    break;
                case TIMEOUT:
                    payment.recordFailure(result.getFailureCode(), result.getFailureMessage());
                    break;
                case ERROR:
                    payment.recordFailure(result.getFailureCode(), result.getFailureMessage());
                    break;
                default:
                    break;
            }

            return paymentRepository.save(payment);
        });

        return new CreatePaymentResponse(updated.getId(), updated.getStatus());
    }
}
