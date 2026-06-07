package com.jpay.merchant.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bill")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bill {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "biller_id", nullable = false)
    private Long billerId;

    @Column(name = "bill_code", nullable = false, unique = true, length = 80)
    private String billCode;

    @Column(name = "bill_title", nullable = false, length = 200)
    private String billTitle;

    @Column(name = "bill_type", nullable = false, length = 30)
    private String billType;         // MONTHLY | ONE_TIME | SEMESTER | EXAM | ADMISSION | FORM_FILLUP | CUSTOM

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT | PUBLISHED | INACTIVE | ARCHIVED

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "parent_bill_id")
    private Long parentBillId;

    @Column(name = "bill_start_date", nullable = false)
    private LocalDate billStartDate;

    @Column(name = "bill_end_date", nullable = false)
    private LocalDate billEndDate;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "bill_description", columnDefinition = "CLOB")
    private String billDescription;

    @Column(name = "institution_section_id")
    private Long institutionSectionId;

    @Column(name = "classification_json", columnDefinition = "CLOB")
    private String classificationJson;

    @Column(name = "uni_pay_mode", length = 20)
    private String uniPayMode;       // ONE_TIME | SEMESTER | MONTHLY

    @Column(name = "allow_partial_payment")
    @Builder.Default
    private Boolean allowPartialPayment = false;

    @Column(name = "allow_month_selection")
    @Builder.Default
    private Boolean allowMonthSelection = true;

    @Column(name = "student_id_type", length = 30)
    @Builder.Default
    private String studentIdType = "STUDENT_ID";

    @Column(name = "is_open_admission")
    @Builder.Default
    private Boolean isOpenAdmission = false;

    @Column(name = "require_form_fill")
    @Builder.Default
    private Boolean requireFormFill = false;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ── Relationships ──────────────────────────────────
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BillMonth> months = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BillFee> fees = new ArrayList<>();

    @OneToOne(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BillLpf lpf;

    @OneToOne(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BillFormConfig formConfig;

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}