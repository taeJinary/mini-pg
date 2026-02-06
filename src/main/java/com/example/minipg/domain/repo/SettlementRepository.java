package com.example.minipg.domain.repo;

import java.time.LocalDate;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.minipg.domain.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, String>, JpaSpecificationExecutor<Settlement> {
    boolean existsByMerchantIdAndSettlementDate(String merchantId, LocalDate settlementDate);
    Optional<Settlement> findByMerchantIdAndSettlementDate(String merchantId, LocalDate settlementDate);
    long countByMerchantIdAndSettlementDate(String merchantId, LocalDate settlementDate);

    @EntityGraph(attributePaths = {"merchant"})
    Page<Settlement> findAll(Specification<Settlement> spec, Pageable pageable);
}
