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
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.services.AccountService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<AccountResponse>> list(Principal principal) {
        return ResponseEntity.ok(accountService.list(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<AccountResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(accountService.get(principal.getName(), id));
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<AccountBalanceResponse> balance(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(accountService.balance(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountCreateRequest request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(principal.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<AccountResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(accountService.update(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> deactivate(@PathVariable Long id, Principal principal) {
        accountService.deactivate(principal.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Conta desativada com sucesso."));
    }
}
