package br.com.kuntzedevprojects.money_master_2.controllers.finance.payable;

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
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.auth.MessageResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.services.finance.payable.MonthlyPayableService;
import jakarta.validation.Valid;

@RestController
public class MonthlyPayableController {

    private final MonthlyPayableService payableService;

    public MonthlyPayableController(MonthlyPayableService payableService) {
        this.payableService = payableService;
    }

    @GetMapping("/monthly-cycles/{cycleId}/payables")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<MonthlyPayableResponse>> list(@PathVariable Long cycleId, Principal principal) {
        return ResponseEntity.ok(payableService.list(principal.getName(), cycleId));
    }

    @PostMapping("/monthly-cycles/{cycleId}/payables")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPayableResponse> create(
            @PathVariable Long cycleId,
            @Valid @RequestBody MonthlyPayableCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payableService.create(principal.getName(), cycleId, request));
    }

    @PutMapping("/payables/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyPayableResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MonthlyPayableUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(payableService.update(principal.getName(), id, request));
    }

    @DeleteMapping("/payables/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> cancel(@PathVariable Long id, Principal principal) {
        payableService.cancel(principal.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Conta mensal cancelada com sucesso."));
    }
}
