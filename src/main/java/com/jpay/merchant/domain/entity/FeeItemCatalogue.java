package com.jpay.merchant.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fee_item_catalogue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeeItemCatalogue {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "biller_id", nullable = false)
    private Long billerId;

    @Column(name = "fee_code", nullable = false, length = 50)
    private String feeCode;

    @Column(name = "fee_name", nullable = false, length = 150)
    private String feeName;

    @Column(name = "account_head", length = 150)
    private String accountHead;

    @Column(name = "default_account_id")
    private Long defaultAccountId;

    @Column(name = "applicable_section", length = 20)
    private String applicableSection;  // SCHOOL | COLLEGE | UNIVERSITY | ALL

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}