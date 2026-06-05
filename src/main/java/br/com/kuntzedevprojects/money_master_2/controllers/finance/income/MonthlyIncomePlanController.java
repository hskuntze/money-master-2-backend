package br.com.kuntzedevprojects.money_master_2.controllers.finance.income;

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
import br.com.kuntzedevprojects.money_master_2.dtos.finance.income.MonthlyIncomePlanCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.income.MonthlyIncomePlanResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.income.MonthlyIncomePlanUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.services.finance.income.MonthlyIncomePlanService;
import jakarta.validation.Valid;

@RestController
public class MonthlyIncomePlanController {

    private final MonthlyIncomePlanService incomePlanService;

    public MonthlyIncomePlanController(MonthlyIncomePlanService incomePlanService) {
        this.incomePlanService = incomePlanService;
    }

    @GetMapping("/monthly-cycles/{cycleId}/income-plans")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<MonthlyIncomePlanResponse>> list(@PathVariable Long cycleId, Principal principal) {
        return ResponseEntity.ok(incomePlanService.list(principal.getName(), cycleId));
    }

    @PostMapping("/monthly-cycles/{cycleId}/income-plans")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyIncomePlanResponse> create(
            @PathVariable Long cycleId,
            @Valid @RequestBody MonthlyIncomePlanCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incomePlanService.create(principal.getName(), cycleId, request));
    }

    @PutMapping("/income-plans/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyIncomePlanResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MonthlyIncomePlanUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(incomePlanService.update(principal.getName(), id, request));
    }

    @DeleteMapping("/income-plans/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MessageResponse> cancel(@PathVariable Long id, Principal principal) {
        incomePlanService.cancel(principal.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Receita mensal cancelada com sucesso."));
    }
}
