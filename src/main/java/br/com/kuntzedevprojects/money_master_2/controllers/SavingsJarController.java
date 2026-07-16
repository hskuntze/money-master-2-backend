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
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarApplyYieldResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarMovementRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarMovementResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarYieldCorrectionRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarYieldCorrectionResponse;
import br.com.kuntzedevprojects.money_master_2.services.SavingsJarService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/savings-jars")
public class SavingsJarController {

    private final SavingsJarService savingsJarService;

    public SavingsJarController(SavingsJarService savingsJarService) {
        this.savingsJarService = savingsJarService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<SavingsJarResponse>> list(Principal principal) {
        return ResponseEntity.ok(savingsJarService.list(principal.getName()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<SavingsJarSummaryResponse> summary(Principal principal) {
        return ResponseEntity.ok(savingsJarService.summary(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<SavingsJarResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(savingsJarService.get(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<SavingsJarResponse> create(
            @Valid @RequestBody SavingsJarCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savingsJarService.create(principal.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<SavingsJarResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SavingsJarUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(savingsJarService.update(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id, Principal principal) {
        boolean deleted = savingsJarService.delete(principal.getName(), id);
        String message = deleted
                ? "Cofrinho excluido com sucesso."
                : "Cofrinho arquivado. O historico de movimentacoes foi preservado.";
        return ResponseEntity.ok(new MessageResponse(message));
    }

    @GetMapping("/{id}/movements")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<SavingsJarMovementResponse>> movements(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(savingsJarService.movements(principal.getName(), id));
    }

    @PostMapping("/{id}/deposits")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<SavingsJarMovementResponse> deposit(
            @PathVariable Long id,
            @Valid @RequestBody SavingsJarMovementRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savingsJarService.deposit(principal.getName(), id, request));
    }

    @PostMapping("/{id}/withdrawals")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<SavingsJarMovementResponse> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody SavingsJarMovementRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savingsJarService.withdraw(principal.getName(), id, request));
    }

    @PostMapping("/{id}/yields")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<SavingsJarMovementResponse> registerManualYield(
            @PathVariable Long id,
            @Valid @RequestBody SavingsJarMovementRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savingsJarService.registerManualYield(principal.getName(), id, request));
    }

    @PostMapping("/{id}/yield/corrections")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<SavingsJarYieldCorrectionResponse> correctYield(
            @PathVariable Long id,
            @Valid @RequestBody SavingsJarYieldCorrectionRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savingsJarService.correctYield(principal.getName(), id, request));
    }

    @PostMapping("/{id}/yield/apply")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<SavingsJarApplyYieldResponse> applyPendingYield(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal
    ) {
        return ResponseEntity.ok(savingsJarService.applyPendingYield(principal.getName(), id, to));
    }

    @PostMapping("/yield/apply-pending")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<List<SavingsJarApplyYieldResponse>> applyPendingYieldsForUser(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal
    ) {
        return ResponseEntity.ok(savingsJarService.applyPendingYieldsForUser(principal.getName(), to));
    }
}
