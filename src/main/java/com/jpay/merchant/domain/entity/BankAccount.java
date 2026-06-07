package com.jpay.merchant.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_account")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankAccount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "biller_id", nullable = false)
    private Long billerId;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "branch_name", length = 150)
    private String branchName;

    @Column(name = "account_no", nullable = false, length = 50)
    private String accountNo;

    @Column(name = "account_name", length = 200)
    private String accountName;

    @Column(name = "routing_no", length = 30)
    private String routingNo;

    @Column(name = "account_type", length = 30)
    private String accountType;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Display label for dropdowns */
    @Transient
    public String getDropdownLabel() {
        return bankName + " — A/C " + maskAccountNo();
    }

    private String maskAccountNo() {
        if (accountNo == null || accountNo.length() <= 4) return accountNo;
        return "****" + accountNo.substring(accountNo.length() - 4);
    }
}