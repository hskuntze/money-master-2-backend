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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentEntryPaymentRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationPreviewResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseEntryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseResponse;
import br.com.kuntzedevprojects.money_master_2.services.InstallmentPurchaseService;
import br.com.kuntzedevprojects.money_master_2.services.finance.installment.InstallmentAnticipationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/installment-purchases")
public class InstallmentPurchaseController {

    private final InstallmentPurchaseService installmentPurchaseService;
    private final InstallmentAnticipationService anticipationService;

    public InstallmentPurchaseController(InstallmentPurchaseService installmentPurchaseService, InstallmentAnticipationService anticipationService) {
        this.installmentPurchaseService = installmentPurchaseService;
        this.anticipationService = anticipationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<InstallmentPurchaseResponse>> list(Principal principal) {
        return ResponseEntity.ok(installmentPurchaseService.list(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<InstallmentPurchaseResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(installmentPurchaseService.get(principal.getName(), id));
    }

    @GetMapping("/{id}/installments")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<InstallmentPurchaseEntryResponse>> listInstallments(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(installmentPurchaseService.listInstallments(principal.getName(), id));
    }

    @GetMapping("/{id}/anticipation-preview")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<InstallmentAnticipationPreviewResponse> anticipationPreview(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam List<Long> installmentIds,
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.math.BigDecimal discountAmount,
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.math.BigDecimal anticipatedAmount,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long targetInvoiceId,
            Principal principal
    ) {
        return ResponseEntity.ok(anticipationService.preview(principal.getName(), id, installmentIds, discountAmount, anticipatedAmount, targetInvoiceId));
    }

    @PostMapping("/{id}/anticipations")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InstallmentAnticipationResponse> anticipate(
            @PathVariable Long id,
            @Valid @RequestBody InstallmentAnticipationRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(anticipationService.anticipate(principal.getName(), id, request));
    }

    @GetMapping("/{id}/anticipations")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<InstallmentAnticipationResponse>> listAnticipations(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(anticipationService.list(principal.getName(), id));
    }

    @PostMapping("/anticipations/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InstallmentAnticipationResponse> cancelAnticipation(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(anticipationService.cancel(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InstallmentPurchaseResponse> create(
            @Valid @RequestBody InstallmentPurchaseCreateRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(installmentPurchaseService.create(principal.getName(), request));
    }

    @PostMapping("/entries/{entryId}/payment")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InstallmentPurchaseResponse> markEntryPaid(
            @PathVariable Long entryId,
            @RequestBody(required = false) InstallmentEntryPaymentRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(installmentPurchaseService.markEntryPaid(principal.getName(), entryId, request));
    }

    @DeleteMapping("/entries/{entryId}/payment")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InstallmentPurchaseResponse> reopenEntryPayment(@PathVariable Long entryId, Principal principal) {
        return ResponseEntity.ok(installmentPurchaseService.reopenEntryPayment(principal.getName(), entryId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> cancel(@PathVariable Long id, Principal principal) {
        installmentPurchaseService.cancel(principal.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Compra parcelada cancelada com sucesso."));
    }
}
