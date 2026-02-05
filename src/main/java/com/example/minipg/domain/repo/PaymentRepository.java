package com.example.minipg.domain.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.minipg.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByOrderId(String orderId);
    long countByOrderId(String orderId);
}
