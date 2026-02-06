package com.example.minipg.domain.repo;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.minipg.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByOrderId(String orderId);
    long countByOrderId(String orderId);
    Optional<Payment> findByPgTransactionId(String pgTransactionId);

    @EntityGraph(attributePaths = {"merchant", "order"})
    Page<Payment> findAll(Specification<Payment> spec, Pageable pageable);
}
