package br.com.kuntzedevprojects.money_master_2.controllers.finance.planning;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemInvoiceLinkRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemLinkTransactionRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemPaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemReopenRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUnlinkTransactionRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileResponse;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;
import br.com.kuntzedevprojects.money_master_2.services.MonthlyPlanReconciliationService;
import jakarta.validation.Valid;

@RestController
public class MonthlyCyclePlanItemController {

    private final FinancialPeriodService financialPeriodService;
    private final MonthlyPlanReconciliationService reconciliationService;

    public MonthlyCyclePlanItemController(
            FinancialPeriodService financialPeriodService,
            MonthlyPlanReconciliationService reconciliationService
    ) {
        this.financialPeriodService = financialPeriodService;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/monthly-cycles/{cycleId}/plan-items")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<MonthlyPlanItemResponse>> listPlanItems(
            @PathVariable Long cycleId,
            @RequestParam(required = false) MonthlyPlanItemStatus status,
            Principal principal
    ) {
        return ResponseEntity.ok(financialPeriodService.listPlanItems(principal.getName(), cycleId, status));
    }

    @PostMapping("/monthly-cycles/{cycleId}/plan-items")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> createPlanItem(
            @PathVariable Long cycleId,
            @Valid @RequestBody MonthlyPlanItemCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financialPeriodService.createPlanItem(principal.getName(), cycleId, request));
    }

    @PutMapping("/monthly-cycles/plan-items/{itemId}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> updatePlanItem(
            @PathVariable Long itemId,
            @Valid @RequestBody MonthlyPlanItemUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(financialPeriodService.updatePlanItem(principal.getName(), itemId, request));
    }

    @DeleteMapping("/monthly-cycles/plan-items/{itemId}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> cancelPlanItem(@PathVariable Long itemId, Principal principal) {
        financialPeriodService.cancelPlanItem(principal.getName(), itemId);
        return ResponseEntity.ok(new MessageResponse("Item do planejamento cancelado com sucesso."));
    }

    @GetMapping("/monthly-cycles/plan-items/{invoiceItemId}/child-candidates")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<MonthlyPlanItemResponse>> listInvoiceChildCandidates(
            @PathVariable Long invoiceItemId,
            Principal principal
    ) {
        return ResponseEntity.ok(financialPeriodService.listInvoiceChildCandidates(principal.getName(), invoiceItemId));
    }

    @PostMapping("/monthly-cycles/plan-items/{invoiceItemId}/children/{childItemId}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> linkPlanItemToInvoice(
            @PathVariable Long invoiceItemId,
            @PathVariable Long childItemId,
            @RequestBody(required = false) MonthlyPlanItemInvoiceLinkRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(
                financialPeriodService.linkPlanItemToInvoice(principal.getName(), invoiceItemId, childItemId, request));
    }

    @DeleteMapping("/monthly-cycles/plan-items/{childItemId}/parent")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> unlinkPlanItemFromInvoice(
            @PathVariable Long childItemId,
            Principal principal
    ) {
        return ResponseEntity.ok(financialPeriodService.unlinkPlanItemFromInvoice(principal.getName(), childItemId));
    }

    @PostMapping("/monthly-cycles/plan-items/{itemId}/payments")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> registerPayment(
            @PathVariable Long itemId,
            @Valid @RequestBody(required = false) MonthlyPlanItemPaymentRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.registerPayment(principal.getName(), itemId, request));
    }

    @PostMapping("/monthly-cycles/plan-items/{itemId}/reopen")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> reopenPlanItem(
            @PathVariable Long itemId,
            @Valid @RequestBody(required = false) MonthlyPlanItemReopenRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.reopenPlanItem(principal.getName(), itemId, request));
    }

    @PostMapping("/monthly-cycles/plan-items/{itemId}/unlink-transaction")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> unlinkTransaction(
            @PathVariable Long itemId,
            @Valid @RequestBody MonthlyPlanItemUnlinkTransactionRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.unlinkTransaction(principal.getName(), itemId, request));
    }

    @PostMapping("/monthly-cycles/plan-items/{itemId}/link-transaction")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> linkTransaction(
            @PathVariable Long itemId,
            @Valid @RequestBody MonthlyPlanItemLinkTransactionRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.linkTransaction(principal.getName(), itemId, request));
    }

    @GetMapping("/monthly-cycles/{cycleId}/unlinked-transactions")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<FinancialTransactionResponse>> listUnlinkedTransactions(
            @PathVariable Long cycleId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "true") boolean onlyUnlinked,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.listTransactionsForAssociation(
                principal.getName(), cycleId, type, onlyUnlinked));
    }

    @PostMapping("/monthly-cycles/{cycleId}/reconcile-transactions")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanReconcileResponse> reconcileTransactions(
            @PathVariable Long cycleId,
            @RequestBody(required = false) MonthlyPlanReconcileRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.reconcile(principal.getName(), cycleId, request));
    }
}
