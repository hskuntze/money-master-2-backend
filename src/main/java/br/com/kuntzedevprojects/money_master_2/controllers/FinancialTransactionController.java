package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.services.FinancialTransactionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions")
public class FinancialTransactionController {

    private final FinancialTransactionService transactionService;

    public FinancialTransactionController(FinancialTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<FinancialTransactionResponse>> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) Long planItemId,
            Principal principal
    ) {
        return ResponseEntity.ok(transactionService.search(principal.getName(), from, to, accountId, categoryId, type, periodId, planItemId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<FinancialTransactionResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(transactionService.get(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialTransactionResponse> create(
            @Valid @RequestBody FinancialTransactionCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(principal.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<FinancialTransactionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FinancialTransactionUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(transactionService.update(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id, Principal principal) {
        transactionService.delete(principal.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Lançamento excluído com sucesso."));
    }
}
