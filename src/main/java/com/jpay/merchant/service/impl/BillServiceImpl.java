package com.jpay.merchant.service.impl;

import com.jpay.merchant.domain.entity.*;
import com.jpay.merchant.dto.request.BillRequest;
import com.jpay.merchant.dto.response.BillResponse;
import com.jpay.merchant.repository.BillRepository;
import com.jpay.merchant.service.BillService;
import com.jpay.merchant.util.BillCodeGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final BillCodeGenerator codeGenerator;
    private final ObjectMapper objectMapper;

    // ── Create Draft ────────────────────────────────────────
    @Override
    public BillResponse createDraft(BillRequest req, Long billerId) {
        String code = (req.getBillCode() != null && !req.getBillCode().isBlank())
                      ? req.getBillCode().trim()
                      : codeGenerator.generate();

        Bill bill = Bill.builder()
            .billerId(billerId)
            .billCode(code)
            .billTitle(req.getBillTitle())
            .billType(req.getBillType())
            .status("DRAFT")
            .version(1)
            .billStartDate(req.getBillStartDate())
            .billEndDate(req.getBillEndDate())
            .academicYear(req.getAcademicYear())
            .billDescription(req.getBillDescription())
            .institutionSectionId(null)        // resolve from session if needed
            .classificationJson(buildClassificationJson(req))
            .uniPayMode(req.getUniPayMode())
            .allowPartialPayment(req.isAllowPartialPayment())
            .allowMonthSelection(req.isAllowMonthSelection())
            .studentIdType(req.getStudentIdType() != null ? req.getStudentIdType() : "STUDENT_ID")
            .isOpenAdmission(req.isOpenAdmission())
            .requireFormFill(req.isRequireFormFill())
            .build();

        // Months
        bill.setMonths(buildMonths(req, bill));

        // Fees (with account mapping)
        bill.setFees(buildFees(req, bill));

        // LPF
        if (req.isLpfEnabled()) {
            bill.setLpf(buildLpf(req, bill));
        }

        Bill saved = billRepository.save(bill);
        log.info("Draft created: {} (id={})", saved.getBillCode(), saved.getId());
        return toResponse(saved);
    }

    // ── Update Draft ────────────────────────────────────────
    @Override
    public BillResponse updateDraft(Long billId, BillRequest req, Long billerId) {
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));

        if (!bill.getBillerId().equals(billerId))
            throw new SecurityException("Access denied");
        if ("PUBLISHED".equals(bill.getStatus()))
            throw new IllegalStateException("Cannot edit a published bill that has payments");

        bill.setBillTitle(req.getBillTitle());
        bill.setBillType(req.getBillType());
        bill.setBillStartDate(req.getBillStartDate());
        bill.setBillEndDate(req.getBillEndDate());
        bill.setAcademicYear(req.getAcademicYear());
        bill.setBillDescription(req.getBillDescription());
        bill.setClassificationJson(buildClassificationJson(req));
        bill.setUniPayMode(req.getUniPayMode());
        bill.setUpdatedAt(LocalDateTime.now());

        // Replace months, fees, lpf
        bill.getMonths().clear();
        bill.getMonths().addAll(buildMonths(req, bill));

        bill.getFees().clear();
        bill.getFees().addAll(buildFees(req, bill));

        if (req.isLpfEnabled()) {
            bill.setLpf(buildLpf(req, bill));
        } else {
            bill.setLpf(null);
        }

        return toResponse(billRepository.save(bill));
    }

    // ── Publish ─────────────────────────────────────────────
    @Override
    public BillResponse publish(Long billId, Long billerId) {
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));
        if (!bill.getBillerId().equals(billerId)) throw new SecurityException("Access denied");

        bill.setStatus("PUBLISHED");
        bill.setPublishedAt(LocalDateTime.now());
        return toResponse(billRepository.save(bill));
    }

    // ── Get / List ──────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public BillResponse getById(Long billId) {
        return toResponse(billRepository.findById(billId)
            .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillResponse> listByBiller(Long billerId, String status) {
        List<Bill> bills = (status != null && !status.isBlank())
            ? billRepository.findByBillerAndStatus(billerId, status)
            : billRepository.findByBillerIdAndStatusNotOrderByCreatedAtDesc(billerId, "ARCHIVED");
        return bills.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Delete ──────────────────────────────────────────────
    @Override
    public void delete(Long billId, Long billerId) {
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));
        if (!bill.getBillerId().equals(billerId)) throw new SecurityException("Access denied");
        if ("PUBLISHED".equals(bill.getStatus()))
            throw new IllegalStateException("Cannot delete a published bill");
        billRepository.delete(bill);
    }

    // ── Builders ─────────────────────────────────────────────

    private String buildClassificationJson(BillRequest req) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("section", req.getInstitutionSection());
            // School fields
            putIfPresent(map, "schoolClass",    req.getSchoolClass());
            putIfPresent(map, "shift",          req.getShift());
            putIfPresent(map, "classSection",   req.getClassSection());
            putIfPresent(map, "group",          req.getGroup());
            putIfPresent(map, "version",        req.getVersion());
            // College fields
            putIfPresent(map, "collegeYear",    req.getCollegeYear());
            putIfPresent(map, "collegeGroup",   req.getCollegeGroup());
            putIfPresent(map, "collegeSection", req.getCollegeSection());
            // University fields
            putIfPresent(map, "faculty",        req.getFaculty());
            putIfPresent(map, "department",     req.getDepartment());
            putIfPresent(map, "program",        req.getProgram());
            putIfPresent(map, "yearSemester",   req.getYearSemester());
            putIfPresent(map, "session",        req.getSession());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void putIfPresent(Map<String, Object> map, String key, String val) {
        if (val != null && !val.isBlank()) map.put(key, val);
    }

    private List<BillMonth> buildMonths(BillRequest req, Bill bill) {
        if (req.getSelectedMonths() == null) return new ArrayList<>();
        List<BillMonth> list = new ArrayList<>();
        int seq = 1;
        for (BillRequest.MonthEntry me : req.getSelectedMonths()) {
            // Use reflection-free builder via inner package access
            // (BillMonth is package-private in entity package — adjust visibility as needed)
            list.add(buildMonth(bill, me.getLabel(), me.getYear(), seq++));
        }
        return list;
    }

    private BillMonth buildMonth(Bill bill, String label, Integer year, int seq) {
        BillMonth m = new BillMonth();
        m.setBill(bill);
        m.setMonthLabel(label);
        m.setMonthYear(year);
        m.setMonthSeq(seq);
        m.setIsActive(true);
        return m;
    }

    private List<BillFee> buildFees(BillRequest req, Bill bill) {
        if (req.getFeeRows() == null) return new ArrayList<>();
        List<BillFee> list = new ArrayList<>();
        int idx = 0;
        for (BillRequest.FeeRowRequest fr : req.getFeeRows()) {
            // Account mapping: advanced mode = per-fee list, simple = single account
            Long accountId = resolveAccount(req, idx);

            BillFee fee = new BillFee();
            fee.setBill(bill);
            fee.setCatalogueItemId(fr.getCatalogueItemId());
            fee.setFeeCode(fr.getFeeCode() != null ? fr.getFeeCode()
                          : "CUSTOM_" + (idx + 1));
            fee.setFeeName(fr.getFeeName());
            fee.setAccountHead(fr.getAccountHead());
            fee.setBankAccountId(accountId);
            fee.setSortOrder(idx);
            fee.setIsActive(true);
            fee.setAmounts(buildAmounts(fr, fee));
            list.add(fee);
            idx++;
        }
        return list;
    }

    private Long resolveAccount(BillRequest req, int idx) {
        if (req.isAdvancedMode()
                && req.getFeeAccountIds() != null
                && idx < req.getFeeAccountIds().size()) {
            return req.getFeeAccountIds().get(idx);
        }
        return req.getSingleAccountId();
    }

    private List<BillFeeAmount> buildAmounts(BillRequest.FeeRowRequest fr, BillFee fee) {
        if (fr.getAmounts() == null) return new ArrayList<>();
        List<BillFeeAmount> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : fr.getAmounts().entrySet()) {
            BillFeeAmount a = new BillFeeAmount();
            a.setBillFee(fee);
            a.setBillMonthId(null); // resolved after months persist in full save flow
            // Store month label in a helper field or resolve via month table
            a.setAmount(entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO);
            list.add(a);
        }
        return list;
    }

    private BillLpf buildLpf(BillRequest req, Bill bill) {
        BillLpf lpf = new BillLpf();
        lpf.setBill(bill);
        lpf.setIsEnabled(true);
        lpf.setLpfStartDate(req.getLpfStartDate());
        lpf.setLpfEndDate(req.getLpfEndDate());
        lpf.setFineType(req.getLpfType());
        lpf.setFineAmount(req.getLpfAmount());
        lpf.setFineScope(req.getLpfScope() != null ? req.getLpfScope() : "ENTIRE");
        lpf.setMaxCap(req.getLpfMaxCap());
        lpf.setGraceDays(req.getLpfGraceDays() != null ? req.getLpfGraceDays() : 0);
        lpf.setRecurrence(req.getLpfRecurrence() != null ? req.getLpfRecurrence() : "ONCE");
        lpf.setWaiverRole(req.getLpfWaiverRole());
        return lpf;
    }

    // ── Response Mapper ──────────────────────────────────────
    private BillResponse toResponse(Bill bill) {
        return BillResponse.builder()
            .id(bill.getId())
            .billCode(bill.getBillCode())
            .billTitle(bill.getBillTitle())
            .billType(bill.getBillType())
            .status(bill.getStatus())
            .version(bill.getVersion())
            .billStartDate(bill.getBillStartDate())
            .billEndDate(bill.getBillEndDate())
            .academicYear(bill.getAcademicYear())
            .classificationJson(bill.getClassificationJson())
            .uniPayMode(bill.getUniPayMode())
            .createdAt(bill.getCreatedAt())
            .publishedAt(bill.getPublishedAt())
            .build();
    }
}
