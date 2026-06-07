package com.jpay.merchant.domain.entity;

import jakarta.persistence.*;
import lombok.*;

// ──────────────────────────────────────────────────
// BillMonth — one row per month in the bill
// ──────────────────────────────────────────────────
@Entity @Table(name = "bill_month")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillMonth {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "month_label", nullable = false, length = 30)
    private String monthLabel;   // JAN | FEB | ... | JUL_2026 | SEMESTER_1 | ONCE

    @Column(name = "month_year")
    private Integer monthYear;   // 2025, 2026 — null for ONE_TIME

    @Column(name = "month_seq", nullable = false)
    private Integer monthSeq;    // display order

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
