package com.example.minipg.domain.repo;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.minipg.domain.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, String> {
    boolean existsByMerchantIdAndSettlementDate(String merchantId, LocalDate settlementDate);
}
