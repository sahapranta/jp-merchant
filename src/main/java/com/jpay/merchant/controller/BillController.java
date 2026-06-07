package com.jpay.merchant.controller;

import com.jpay.merchant.domain.entity.BankAccount;
import com.jpay.merchant.domain.entity.FeeItemCatalogue;
import com.jpay.merchant.dto.request.BillRequest;
import com.jpay.merchant.dto.response.BillResponse;
import com.jpay.merchant.repository.BankAccountRepository;
import com.jpay.merchant.repository.FeeItemCatalogueRepository;
import com.jpay.merchant.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bills")
@RequiredArgsConstructor
@Slf4j
public class BillController {

    private final BillService billService;
    private final BankAccountRepository bankAccountRepo;
    private final FeeItemCatalogueRepository catalogueRepo;

    @Value("${app.biller.default-id:1}")
    private Long defaultBillerId;

    // ── GET /bills  (list) ───────────────────────────────────
    @GetMapping
    public String listBills(@RequestParam(required = false) String status,
                            Model model) {
        Long billerId = getSessionBillerId();
        model.addAttribute("bills", billService.listByBiller(billerId, status));
        model.addAttribute("currentStatus", status);
        model.addAttribute("institutionName", "Dhaka Model College");
        model.addAttribute("userName", "Admin");
        return "bill/list";
    }

    // ── GET /bills/create ────────────────────────────────────
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("billRequest", new BillRequest());
        populateFormModel(model);
        return "bill/create";
    }

    // ── GET /bills/{id}  (detail / preview) ──────────────────
    @GetMapping("/{id}")
    public String showBill(@PathVariable Long id, Model model) {
        BillResponse bill = billService.getById(id);
        model.addAttribute("bill", bill);
        model.addAttribute("institutionName", "Dhaka Model College");
        model.addAttribute("userName", "Admin");
        return "bill/detail";
    }

    // ── GET /bills/{id}/edit ─────────────────────────────────
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        BillResponse bill = billService.getById(id);
        // Map BillResponse back to BillRequest for form pre-fill
        model.addAttribute("billRequest", new BillRequest()); // TODO: mapper
        model.addAttribute("existingBill", bill);
        populateFormModel(model);
        return "bill/create";
    }

    // ── POST /bills/draft  (save draft — called via AJAX from JS) ──
    @PostMapping("/draft")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveDraft(
            @Valid @RequestBody BillRequest request,
            BindingResult errors) {

        if (errors.hasErrors()) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "errors", errors.getAllErrors()));
        }
        try {
            Long billerId = getSessionBillerId();
            BillResponse saved = billService.createDraft(request, billerId);
            return ResponseEntity.ok(Map.of(
                "success",  true,
                "billId",   saved.getId(),
                "billCode", saved.getBillCode(),
                "message",  "Draft saved successfully"
            ));
        } catch (Exception e) {
            log.error("Draft save failed", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── POST /bills/{id}/draft  (update existing draft) ─────
    @PostMapping("/{id}/draft")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody BillRequest request,
            BindingResult errors) {

        if (errors.hasErrors()) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "errors", errors.getAllErrors()));
        }
        try {
            BillResponse updated = billService.updateDraft(id, request, getSessionBillerId());
            return ResponseEntity.ok(Map.of(
                "success",  true,
                "billId",   updated.getId(),
                "billCode", updated.getBillCode(),
                "message",  "Draft updated"
            ));
        } catch (Exception e) {
            log.error("Draft update failed", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── POST /bills/{id}/publish ─────────────────────────────
    @PostMapping("/{id}/publish")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> publishBill(@PathVariable Long id) {
        try {
            BillResponse published = billService.publish(id, getSessionBillerId());
            return ResponseEntity.ok(Map.of(
                "success",  true,
                "billId",   published.getId(),
                "billCode", published.getBillCode(),
                "status",   published.getStatus(),
                "message",  "Bill published successfully"
            ));
        } catch (Exception e) {
            log.error("Publish failed for bill {}", id, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── DELETE /bills/{id} ───────────────────────────────────
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteBill(@PathVariable Long id) {
        try {
            billService.delete(id, getSessionBillerId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Bill deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── API: GET /bills/api/accounts ─────────────────────────
    // Called by frontend AJAX when loading account dropdown
    @GetMapping("/api/accounts")
    @ResponseBody
    public List<BankAccount> getAccounts() {
        return bankAccountRepo.findByBillerIdAndIsActiveTrueOrderBySortOrderAsc(getSessionBillerId());
    }

    // ── API: GET /bills/api/fee-defaults?section=COLLEGE ────
    // Returns default fee catalogue items for a given section
    @GetMapping("/api/fee-defaults")
    @ResponseBody
    public List<FeeItemCatalogue> getFeeDefaults(@RequestParam String section) {
        List<String> sections = List.of(section, "ALL");
        return catalogueRepo
            .findByBillerIdAndIsActiveTrueAndApplicableSectionInOrderBySortOrderAsc(
                getSessionBillerId(), sections);
    }

    // ── Helpers ──────────────────────────────────────────────

    private void populateFormModel(Model model) {
        Long billerId = getSessionBillerId();
        List<BankAccount> accounts = bankAccountRepo
            .findByBillerIdAndIsActiveTrueOrderBySortOrderAsc(billerId);
        model.addAttribute("bankAccounts", accounts);
        model.addAttribute("institutionName", "Dhaka Model College"); // TODO: from session
        model.addAttribute("userName",        "Admin");               // TODO: from session
    }

    /**
     * TODO: Replace with real session/auth principal lookup.
     * e.g. ((PortalUserDetails) SecurityContextHolder.getContext()
     *       .getAuthentication().getPrincipal()).getBillerId()
     */
    private Long getSessionBillerId() {
        return defaultBillerId;
    }
}