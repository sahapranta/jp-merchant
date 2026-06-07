package com.jpay.merchant.domain.entity;

import jakarta.persistence.*;
import lombok.*;

// ──────────────────────────────────────────────────
// BillLpf — late payment fine config
// ──────────────────────────────────────────────────
@Entity
@Table(name = "bill_lpf")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillLpf {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "lpf_start_date")
    private java.time.LocalDate lpfStartDate;

    @Column(name = "lpf_end_date")
    private java.time.LocalDate lpfEndDate;

    @Column(name = "fine_type", length = 30)
    private String fineType; // FIXED | PERCENT | DAILY_FIXED | DAILY_PCT

    @Column(name = "fine_amount", precision = 10, scale = 2)
    private java.math.BigDecimal fineAmount;

    @Column(name = "fine_scope", length = 20)
    @Builder.Default
    private String fineScope = "ENTIRE"; // ENTIRE | PER_FEE

    @Column(name = "max_cap", precision = 10, scale = 2)
    private java.math.BigDecimal maxCap;

    @Column(name = "grace_days")
    @Builder.Default
    private Integer graceDays = 0;

    @Column(name = "recurrence", length = 20)
    @Builder.Default
    private String recurrence = "ONCE"; // ONCE | DAILY | MONTHLY

    @Column(name = "waiver_role", length = 30)
    private String waiverRole;
}
