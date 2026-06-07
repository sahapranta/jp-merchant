package com.jpay.merchant.service.impl;

import com.jpay.merchant.dto.request.BillRequest;
import com.jpay.merchant.dto.response.BillResponse;
import com.jpay.merchant.service.BillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BillServiceImplTest {

    @Autowired
    private BillService billService;

    private static final Long BILLER_ID = 1L;
    private static final Long OTHER_BILLER = 999L;

    private BillRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = BillRequest.builder()
            .billTitle("Test Bill – Class 9 Monthly Fee")
            .billType("MONTHLY")
            .billStartDate(LocalDate.of(2026, 1, 1))
            .billEndDate(LocalDate.of(2026, 12, 31))
            .academicYear("2026")
            .institutionSection("COLLEGE")
            .collegeYear("Y1")
            .collegeGroup("SCIENCE")
            .singleAccountId(1L)
            .selectedMonths(List.of(
                new BillRequest.MonthEntry("JAN_2026", 2026, 1),
                new BillRequest.MonthEntry("FEB_2026", 2026, 2)
            ))
            .feeRows(List.of(
                BillRequest.FeeRowRequest.builder()
                    .feeName("Tuition Fee")
                    .feeCode("TUITION_FEE")
                    .accountHead("Revenue – Tuition")
                    .bankAccountId(1L)
                    .amounts(Map.of("JAN_2026", BigDecimal.valueOf(1000), "FEB_2026", BigDecimal.valueOf(1000)))
                    .sortOrder(1)
                    .build(),
                BillRequest.FeeRowRequest.builder()
                    .feeName("Exam Fee")
                    .feeCode("EXAM_FEE")
                    .accountHead("Revenue – Exam")
                    .bankAccountId(2L)
                    .amounts(Map.of("JAN_2026", BigDecimal.valueOf(500), "FEB_2026", BigDecimal.valueOf(500)))
                    .sortOrder(2)
                    .build()
            ))
            .build();
    }

    // ───── createDraft ─────

    @Test
    void createDraft_shouldPersistBillWithDraftStatus() {
        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);

        assertNotNull(resp.getId());
        assertEquals("DRAFT", resp.getStatus());
        assertEquals(validRequest.getBillTitle(), resp.getBillTitle());
        assertEquals(validRequest.getBillType(), resp.getBillType());
        assertNotNull(resp.getBillCode());
        assertTrue(resp.getBillCode().startsWith("BILL-"));
        assertNotNull(resp.getCreatedAt());
    }

    @Test
    void createDraft_shouldPersistMonthsAndFees() {
        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);

        assertEquals(2, resp.getMonths().size());
        assertEquals("JAN_2026", resp.getMonths().get(0).getLabel());
        assertEquals("FEB_2026", resp.getMonths().get(1).getLabel());

        assertEquals(2, resp.getFees().size());
        BillResponse.FeeDto fee1 = resp.getFees().get(0);
        assertEquals("Tuition Fee", fee1.getFeeName());
        assertEquals(BigDecimal.valueOf(1000), fee1.getAmounts().get("JAN_2026"));
    }

    @Test
    void createDraft_shouldUseProvidedBillCode() {
        validRequest.setBillCode("MY-CUSTOM-CODE");
        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);
        assertEquals("MY-CUSTOM-CODE", resp.getBillCode());
    }

    @Test
    void createDraft_shouldGenerateBillCodeWhenBlank() {
        validRequest.setBillCode("");
        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);
        assertNotNull(resp.getBillCode());
        assertTrue(resp.getBillCode().startsWith("BILL-"));
    }

    @Test
    void createDraft_shouldSetClassificationJson() {
        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);
        assertNotNull(resp.getClassificationJson());
        assertTrue(resp.getClassificationJson().contains("\"section\":\"COLLEGE\""));
        assertTrue(resp.getClassificationJson().contains("\"collegeYear\":\"Y1\""));
    }

    @Test
    void createDraft_shouldSetInstitutionSection() {
        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);
        assertEquals("COLLEGE", resp.getInstitutionSection());
    }

    // ───── getById ─────

    @Test
    void getById_shouldReturnBill() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        BillResponse fetched = billService.getById(created.getId());

        assertEquals(created.getId(), fetched.getId());
        assertEquals(created.getBillTitle(), fetched.getBillTitle());
        assertEquals(created.getBillCode(), fetched.getBillCode());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        assertThrows(IllegalArgumentException.class, () -> billService.getById(99999L));
    }

    // ───── listByBiller ─────

    @Test
    void listByBiller_shouldReturnAllNonArchivedBills() {
        billService.createDraft(validRequest, BILLER_ID);
        billService.createDraft(validRequest, BILLER_ID);

        List<BillResponse> bills = billService.listByBiller(BILLER_ID, null);
        assertTrue(bills.size() >= 2);
    }

    @Test
    void listByBiller_shouldFilterByStatus() {
        BillResponse draft = billService.createDraft(validRequest, BILLER_ID);
        billService.publish(draft.getId(), BILLER_ID);

        List<BillResponse> drafts = billService.listByBiller(BILLER_ID, "DRAFT");
        assertTrue(drafts.stream().noneMatch(b -> b.getStatus().equals("PUBLISHED")));

        List<BillResponse> published = billService.listByBiller(BILLER_ID, "PUBLISHED");
        assertTrue(published.stream().allMatch(b -> b.getStatus().equals("PUBLISHED")));
    }

    @Test
    void listByBiller_shouldNotReturnArchived() {
        billService.createDraft(validRequest, BILLER_ID);
        List<BillResponse> all = billService.listByBiller(BILLER_ID, null);
        assertTrue(all.stream().noneMatch(b -> "ARCHIVED".equals(b.getStatus())));
    }

    @Test
    void listByBiller_shouldReturnEmptyForUnknownBiller() {
        List<BillResponse> bills = billService.listByBiller(OTHER_BILLER, null);
        assertTrue(bills.isEmpty());
    }

    // ───── updateDraft ─────

    @Test
    void updateDraft_shouldUpdateFields() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);

        validRequest.setBillTitle("Updated Title");
        validRequest.setBillDescription("Updated description");

        BillResponse updated = billService.updateDraft(created.getId(), validRequest, BILLER_ID);

        assertEquals("Updated Title", updated.getBillTitle());
        assertEquals("Updated description", updated.getBillDescription());
        assertEquals(created.getBillCode(), updated.getBillCode()); // code is immutable after creation
        assertEquals(created.getId(), updated.getId());
    }

    @Test
    void updateDraft_shouldReplaceMonthsAndFees() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);

        validRequest.setSelectedMonths(List.of(
            new BillRequest.MonthEntry("MAR_2026", 2026, 1)
        ));
        validRequest.setFeeRows(List.of(
            BillRequest.FeeRowRequest.builder()
                .feeName("Lab Fee")
                .feeCode("LAB_FEE")
                .amounts(Map.of("MAR_2026", BigDecimal.valueOf(2000)))
                .sortOrder(1)
                .build()
        ));

        BillResponse updated = billService.updateDraft(created.getId(), validRequest, BILLER_ID);

        assertEquals(1, updated.getMonths().size());
        assertEquals("MAR_2026", updated.getMonths().get(0).getLabel());
        assertEquals(1, updated.getFees().size());
        assertEquals("Lab Fee", updated.getFees().get(0).getFeeName());
    }

    @Test
    void updateDraft_shouldThrowWhenNotFound() {
        assertThrows(IllegalArgumentException.class,
            () -> billService.updateDraft(99999L, validRequest, BILLER_ID));
    }

    @Test
    void updateDraft_shouldThrowWhenBillerMismatch() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        assertThrows(SecurityException.class,
            () -> billService.updateDraft(created.getId(), validRequest, OTHER_BILLER));
    }

    @Test
    void updateDraft_shouldThrowWhenPublished() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        billService.publish(created.getId(), BILLER_ID);
        assertThrows(IllegalStateException.class,
            () -> billService.updateDraft(created.getId(), validRequest, BILLER_ID));
    }

    // ───── publish ─────

    @Test
    void publish_shouldSetStatusToPublished() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        BillResponse published = billService.publish(created.getId(), BILLER_ID);

        assertEquals("PUBLISHED", published.getStatus());
        assertNotNull(published.getPublishedAt());
    }

    @Test
    void publish_shouldThrowWhenBillerMismatch() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        assertThrows(SecurityException.class,
            () -> billService.publish(created.getId(), OTHER_BILLER));
    }

    // ───── delete ─────

    @Test
    void delete_shouldRemoveDraft() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        billService.delete(created.getId(), BILLER_ID);
        assertThrows(IllegalArgumentException.class, () -> billService.getById(created.getId()));
    }

    @Test
    void delete_shouldThrowWhenBillerMismatch() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        assertThrows(SecurityException.class,
            () -> billService.delete(created.getId(), OTHER_BILLER));
    }

    @Test
    void delete_shouldThrowWhenPublished() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        billService.publish(created.getId(), BILLER_ID);
        assertThrows(IllegalStateException.class,
            () -> billService.delete(created.getId(), BILLER_ID));
    }

    // ───── LPF ─────

    @Test
    void createDraft_shouldPersistLpfWhenEnabled() {
        validRequest.setLpfEnabled(true);
        validRequest.setLpfStartDate(LocalDate.of(2026, 2, 1));
        validRequest.setLpfEndDate(LocalDate.of(2026, 6, 30));
        validRequest.setLpfType("FIXED");
        validRequest.setLpfAmount(BigDecimal.valueOf(100));
        validRequest.setLpfScope("ENTIRE");
        validRequest.setLpfGraceDays(5);
        validRequest.setLpfRecurrence("ONCE");

        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);

        assertNotNull(resp.getLpf());
        assertTrue(resp.getLpf().getIsEnabled());
        assertEquals("FIXED", resp.getLpf().getFineType());
        assertEquals(BigDecimal.valueOf(100), resp.getLpf().getFineAmount());
    }

    @Test
    void createDraft_shouldNotPersistLpfWhenDisabled() {
        validRequest.setLpfEnabled(false);
        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);
        assertNull(resp.getLpf());
    }

    @Test
    void updateDraft_shouldAddLpf() {
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        assertNull(created.getLpf());

        validRequest.setLpfEnabled(true);
        validRequest.setLpfStartDate(LocalDate.of(2026, 3, 1));
        validRequest.setLpfAmount(BigDecimal.valueOf(50));
        validRequest.setLpfType("PERCENT");
        validRequest.setLpfScope("ENTIRE");

        BillResponse updated = billService.updateDraft(created.getId(), validRequest, BILLER_ID);
        assertNotNull(updated.getLpf());
        assertTrue(updated.getLpf().getIsEnabled());
    }

    @Test
    void updateDraft_shouldRemoveLpf() {
        validRequest.setLpfEnabled(true);
        validRequest.setLpfStartDate(LocalDate.of(2026, 3, 1));
        validRequest.setLpfAmount(BigDecimal.valueOf(50));
        validRequest.setLpfType("FIXED");
        BillResponse created = billService.createDraft(validRequest, BILLER_ID);
        assertNotNull(created.getLpf());

        validRequest.setLpfEnabled(false);
        BillResponse updated = billService.updateDraft(created.getId(), validRequest, BILLER_ID);
        assertNull(updated.getLpf());
    }

    // ───── grandTotal ─────

    @Test
    void createDraft_shouldCalculateGrandTotal() {
        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);
        // Tuition: 1000+1000=2000, Exam: 500+500=1000 → total 3000
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(resp.getGrandTotal()));
    }

    // ───── ONE_TIME bill ─────

    @Test
    void createDraft_oneTimeBillUsesOnesMonth() {
        validRequest.setBillType("ONE_TIME");
        validRequest.setSelectedMonths(List.of(
            new BillRequest.MonthEntry("ONCE", null, 1)
        ));
        validRequest.setFeeRows(List.of(
            BillRequest.FeeRowRequest.builder()
                .feeName("Admission Fee")
                .feeCode("ADMISSION_FEE")
                .amounts(Map.of("ONCE", BigDecimal.valueOf(5000)))
                .sortOrder(1)
                .build()
        ));

        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);
        assertEquals(1, resp.getMonths().size());
        assertEquals("ONCE", resp.getMonths().get(0).getLabel());
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(resp.getGrandTotal()));
    }

    // ───── empty fee rows fallback account ─────

    @Test
    void createDraft_shouldFallbackToDefaultAccountWhenNull() {
        validRequest.setSingleAccountId(null);
        BillResponse resp = billService.createDraft(validRequest, BILLER_ID);
        assertNotNull(resp.getId());
        // Account resolved from first active bank account (id=1 in seed data)
        assertNotNull(resp.getFees().get(0).getBankAccountId());
    }
}
