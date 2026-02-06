package com.example.minipg.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "settlements",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_settlements_merchant_date", columnNames = {"merchant_id", "settlement_date"})
    },
    indexes = {
        @Index(name = "idx_settlements_merchant_date", columnList = "merchant_id, settlement_date")
    }
)
public class Settlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "gross_amount", nullable = false)
    private long grossAmount;

    @Column(name = "fee_amount", nullable = false)
    private long feeAmount;

    @Column(name = "net_amount", nullable = false)
    private long netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.READY;

    public static Settlement create(
        Merchant merchant,
        LocalDate settlementDate,
        long grossAmount,
        long feeAmount,
        long netAmount
    ) {
        Settlement settlement = new Settlement();
        settlement.merchant = merchant;
        settlement.settlementDate = settlementDate;
        settlement.grossAmount = grossAmount;
        settlement.feeAmount = feeAmount;
        settlement.netAmount = netAmount;
        settlement.status = SettlementStatus.READY;
        return settlement;
    }
}
