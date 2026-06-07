package com.jpay.merchant.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Form-binding DTO for bill creation / edit.
 * Bound via th:object="*{billRequest}" in the Thymeleaf template.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillRequest {

    // ── Step 1: Bill Info ──────────────────────────────────
    @NotBlank(message = "Bill title is required")
    @Size(max = 200, message = "Title too long")
    private String billTitle;

    @NotBlank(message = "Bill type is required")
    private String billType;

    @NotNull(message = "Start date is required")
    private LocalDate billStartDate;

    @NotNull(message = "End date is required")
    private LocalDate billEndDate;

    private String academicYear;
    private String billCode;          // auto-generated if blank
    private String billDescription;

    // ── Step 2: Classification ─────────────────────────────
    @NotBlank(message = "Institution section is required")
    private String institutionSection;   // SCHOOL | COLLEGE | UNIVERSITY

    // School
    private String schoolClass;
    private String shift;
    private String classSection;
    private String group;
    private String version;

    // College
    private String collegeYear;
    private String collegeGroup;
    private String collegeSection;
    private String collegeShift;
    private String collegeVersion;

    // University
    private String faculty;
    private String department;
    private String program;
    private String yearSemester;
    private String session;

    // University payment mode
    private String uniPayMode;   // ONE_TIME | SEMESTER | MONTHLY

    // ── Step 3: Fee Structure ──────────────────────────────
    // Selected months (e.g. ["JAN_2025","MAR_2025"]) — flexible, not limited to 12
    @Builder.Default
    private List<MonthEntry> selectedMonths = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<FeeRowRequest> feeRows = new ArrayList<>();

    // Semester blocks (university semester mode)
    @Valid
    @Builder.Default
    private List<SemesterBlock> semesterBlocks = new ArrayList<>();

    // ── Step 4: Account Mapping ────────────────────────────
    // Simple mode: one account for all
    private Long singleAccountId;

    // Advanced mode: per-fee mapping — index matches feeRows list
    @Builder.Default
    private List<Long> feeAccountIds = new ArrayList<>();

    // ── Step 5: LPF ────────────────────────────────────────
    private boolean lpfEnabled;
    private LocalDate lpfStartDate;
    private LocalDate lpfEndDate;
    private String lpfType;         // FIXED | PERCENT | DAILY_FIXED | DAILY_PCT
    private BigDecimal lpfAmount;
    private String lpfScope;        // ENTIRE | PER_FEE
    private BigDecimal lpfMaxCap;
    private Integer lpfGraceDays;
    private String lpfRecurrence;   // ONCE | DAILY | MONTHLY
    private String lpfWaiverRole;

    // Which fee rows have LPF applied (indices, for PER_FEE scope)
    @Builder.Default
    private List<Integer> lpfFeeIndices = new ArrayList<>();

    // ── Bill config ─────────────────────────────────────────
    private boolean advancedMode;
    private boolean allowPartialPayment;
    private boolean allowMonthSelection;
    private String  studentIdType;
    private boolean isOpenAdmission;
    private boolean requireFormFill;

    // ── Nested DTOs ─────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthEntry {
        private String label;      // e.g. JAN | FEB | JUL_2026 | ONCE | SEMESTER_1
        private Integer year;      // e.g. 2026 — null for ONE_TIME
        private Integer seq;       // display order
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FeeRowRequest {
        private Long   catalogueItemId;  // null = user-defined
        private String feeCode;
        @NotBlank(message = "Fee name is required")
        private String feeName;
        private String accountHead;
        // accountId resolved from singleAccountId or feeAccountIds
        private Long   bankAccountId;
        // amounts: key = monthLabel (e.g. "JAN"), value = amount
        @Builder.Default
        private java.util.Map<String, BigDecimal> amounts = new java.util.LinkedHashMap<>();
        private Integer sortOrder;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SemesterBlock {
        @NotBlank(message = "Semester name is required")
        private String  name;            // e.g. "1st Semester"
        private BigDecimal tuition;
        private BigDecimal exam;
        private BigDecimal lab;
        private BigDecimal registration;
        private BigDecimal other;
    }
}