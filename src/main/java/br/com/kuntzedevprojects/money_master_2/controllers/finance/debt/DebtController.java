package br.com.kuntzedevprojects.money_master_2.controllers.finance.debt;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtCancelRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.debt.DebtSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.services.finance.debt.DebtService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/debts")
public class DebtController {

    private final DebtService debtService;

    public DebtController(DebtService debtService) {
        this.debtService = debtService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<DebtResponse>> list(Principal principal) {
        return ResponseEntity.ok(debtService.list(principal.getName()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<DebtSummaryResponse> summary(Principal principal) {
        return ResponseEntity.ok(debtService.summary(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<DebtResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(debtService.get(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<DebtResponse> create(@Valid @RequestBody DebtCreateRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debtService.create(principal.getName(), request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<DebtResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DebtCancelRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(debtService.cancel(principal.getName(), id, request));
    }
}
