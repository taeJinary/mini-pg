package com.example.minipg.domain.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.minipg.domain.PaymentEvent;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, String> {
    boolean existsByEventId(String eventId);
}
