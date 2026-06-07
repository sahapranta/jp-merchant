package com.jpay.merchant.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillResponse {
    private Long          id;
    private String        billCode;
    private String        billTitle;
    private String        billType;
    private String        status;
    private Integer       version;
    private LocalDate     billStartDate;
    private LocalDate     billEndDate;
    private String        academicYear;
    private String        institutionSection;
    private String        classificationJson;
    private String        uniPayMode;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private List<MonthDto>  months;
    private List<FeeDto>    fees;
    private LpfDto          lpf;
    private BigDecimal      grandTotal;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthDto {
        private Long    id;
        private String  label;
        private Integer year;
        private Integer seq;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FeeDto {
        private Long    id;
        private String  feeCode;
        private String  feeName;
        private String  accountHead;
        private Long    bankAccountId;
        private String  bankAccountLabel;
        private Map<String, BigDecimal> amounts;   // monthLabel -> amount
        private BigDecimal rowTotal;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LpfDto {
        private Boolean    isEnabled;
        private LocalDate  startDate;
        private LocalDate  endDate;
        private String     fineType;
        private BigDecimal fineAmount;
        private String     fineScope;
        private BigDecimal maxCap;
        private Integer    graceDays;
        private String     recurrence;
        private String     waiverRole;
    }
}
