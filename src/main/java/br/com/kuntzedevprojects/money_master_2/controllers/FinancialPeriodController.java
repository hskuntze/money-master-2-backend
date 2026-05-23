package br.com.kuntzedevprojects.money_master_2.controllers;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodTurnoverRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialPeriodUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPeriodSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemLinkTransactionRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemPaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemReopenRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUnlinkTransactionRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanReconcileResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPlanItemUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.services.FinancialPeriodService;
import br.com.kuntzedevprojects.money_master_2.services.MonthlyPlanReconciliationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/financial-periods")
public class FinancialPeriodController {

    private final FinancialPeriodService financialPeriodService;
    private final MonthlyPlanReconciliationService reconciliationService;

    public FinancialPeriodController(
            FinancialPeriodService financialPeriodService,
            MonthlyPlanReconciliationService reconciliationService
    ) {
        this.financialPeriodService = financialPeriodService;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<FinancialPeriodResponse>> list(Principal principal) {
        return ResponseEntity.ok(financialPeriodService.list(principal.getName()));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<FinancialPeriodResponse> current(Principal principal) {
        return ResponseEntity.ok(financialPeriodService.current(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<FinancialPeriodResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(financialPeriodService.get(principal.getName(), id));
    }

    @PostMapping("/turnover")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialPeriodResponse> turnover(
            @Valid @RequestBody FinancialPeriodTurnoverRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financialPeriodService.turnover(principal.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialPeriodResponse> updatePeriod(
            @PathVariable Long id,
            @Valid @RequestBody FinancialPeriodUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(financialPeriodService.updatePeriod(principal.getName(), id, request));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialPeriodResponse> reopenPeriod(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(financialPeriodService.reopenPeriod(principal.getName(), id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialPeriodResponse> closePeriod(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(financialPeriodService.closePeriodById(principal.getName(), id));
    }

    @GetMapping("/{periodId}/summary")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<MonthlyPeriodSummaryResponse> summary(@PathVariable Long periodId, Principal principal) {
        return ResponseEntity.ok(financialPeriodService.summary(principal.getName(), periodId));
    }

    @GetMapping("/{periodId}/plan-items")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<MonthlyPlanItemResponse>> listPlanItems(
            @PathVariable Long periodId,
            @RequestParam(required = false) MonthlyPlanItemStatus status,
            Principal principal
    ) {
        return ResponseEntity.ok(financialPeriodService.listPlanItems(principal.getName(), periodId, status));
    }

    @PostMapping("/{periodId}/plan-items")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> createPlanItem(
            @PathVariable Long periodId,
            @Valid @RequestBody MonthlyPlanItemCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financialPeriodService.createPlanItem(principal.getName(), periodId, request));
    }

    @PutMapping("/plan-items/{itemId}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> updatePlanItem(
            @PathVariable Long itemId,
            @Valid @RequestBody MonthlyPlanItemUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(financialPeriodService.updatePlanItem(principal.getName(), itemId, request));
    }

    @DeleteMapping("/plan-items/{itemId}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> cancelPlanItem(@PathVariable Long itemId, Principal principal) {
        financialPeriodService.cancelPlanItem(principal.getName(), itemId);
        return ResponseEntity.ok(new MessageResponse("Item do planejamento cancelado com sucesso."));
    }


    @PostMapping("/plan-items/{itemId}/payments")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> registerPayment(
            @PathVariable Long itemId,
            @Valid @RequestBody(required = false) MonthlyPlanItemPaymentRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.registerPayment(principal.getName(), itemId, request));
    }

    @PostMapping("/plan-items/{itemId}/reopen")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> reopenPlanItem(
            @PathVariable Long itemId,
            @Valid @RequestBody(required = false) MonthlyPlanItemReopenRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.reopenPlanItem(principal.getName(), itemId, request));
    }

    @PostMapping("/plan-items/{itemId}/unlink-transaction")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> unlinkTransaction(
            @PathVariable Long itemId,
            @Valid @RequestBody MonthlyPlanItemUnlinkTransactionRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.unlinkTransaction(principal.getName(), itemId, request));
    }

    @PostMapping("/plan-items/{itemId}/link-transaction")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanItemResponse> linkTransaction(
            @PathVariable Long itemId,
            @Valid @RequestBody MonthlyPlanItemLinkTransactionRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.linkTransaction(principal.getName(), itemId, request));
    }

    @GetMapping("/{periodId}/unlinked-transactions")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<FinancialTransactionResponse>> listUnlinkedTransactions(
            @PathVariable Long periodId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "true") boolean onlyUnlinked,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.listTransactionsForAssociation(principal.getName(), periodId, type, onlyUnlinked));
    }

    @PostMapping("/{periodId}/reconcile-transactions")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPlanReconcileResponse> reconcileTransactions(
            @PathVariable Long periodId,
            @RequestBody(required = false) MonthlyPlanReconcileRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(reconciliationService.reconcile(principal.getName(), periodId, request));
    }

}

