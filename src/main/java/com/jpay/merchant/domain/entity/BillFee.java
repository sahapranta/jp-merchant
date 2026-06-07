package com.jpay.merchant.domain.entity;

import jakarta.persistence.*;
import lombok.*;

// ──────────────────────────────────────────────────
// BillFee — one row per fee item in the bill
// ──────────────────────────────────────────────────
@Entity @Table(name = "bill_fee")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillFee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "catalogue_item_id")
    private Long catalogueItemId;   // null if user-defined

    @Column(name = "fee_code", nullable = false, length = 50)
    private String feeCode;

    @Column(name = "fee_name", nullable = false, length = 150)
    private String feeName;

    @Column(name = "account_head", length = 150)
    private String accountHead;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "billFee", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<BillFeeAmount> amounts = new java.util.ArrayList<>();
}