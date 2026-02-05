package com.example.minipg.domain.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.minipg.domain.PaymentEvent;

import java.util.Optional;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, String> {
    boolean existsByEventId(String eventId);
    Optional<PaymentEvent> findByEventId(String eventId);
    long countByEventId(String eventId);
}
