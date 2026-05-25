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
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentPurchaseResponse;
import br.com.kuntzedevprojects.money_master_2.services.InstallmentPurchaseService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/installment-purchases")
public class InstallmentPurchaseController {

    private final InstallmentPurchaseService installmentPurchaseService;

    public InstallmentPurchaseController(InstallmentPurchaseService installmentPurchaseService) {
        this.installmentPurchaseService = installmentPurchaseService;
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
