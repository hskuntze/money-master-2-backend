package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.installments.InstallmentAnticipationResponse;
import br.com.kuntzedevprojects.money_master_2.services.finance.installment.InstallmentAnticipationService;

@RestController
@RequestMapping("/installment-anticipations")
public class InstallmentAnticipationController {

    private final InstallmentAnticipationService anticipationService;

    public InstallmentAnticipationController(InstallmentAnticipationService anticipationService) {
        this.anticipationService = anticipationService;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InstallmentAnticipationResponse> cancel(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(anticipationService.cancel(principal.getName(), id));
    }
}
