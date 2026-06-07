package com.jpay.merchant.domain.entity;

import jakarta.persistence.*;
import lombok.*;

// ──────────────────────────────────────────────────
// BillFeeAmount — amount per fee per month
// ──────────────────────────────────────────────────
@Entity @Table(name = "bill_fee_amount")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillFeeAmount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_fee_id", nullable = false)
    private BillFee billFee;

    @Column(name = "bill_month_id")
    private Long billMonthId;   // null for ONE_TIME

    @Column(name = "amount", nullable = false)
    @Builder.Default
    private java.math.BigDecimal amount = java.math.BigDecimal.ZERO;

    @Column(name = "is_waivable")
    @Builder.Default
    private Boolean isWaivable = false;
}
