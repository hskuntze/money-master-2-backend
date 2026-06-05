package br.com.kuntzedevprojects.money_master_2.controllers.finance.savings;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarContributionPlanRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarContributionPlanResponse;
import br.com.kuntzedevprojects.money_master_2.services.finance.savings.SavingsJarContributionPlanService;
import jakarta.validation.Valid;

@RestController
public class SavingsJarContributionPlanController {

    private final SavingsJarContributionPlanService contributionPlanService;

    public SavingsJarContributionPlanController(SavingsJarContributionPlanService contributionPlanService) {
        this.contributionPlanService = contributionPlanService;
    }

    @GetMapping("/monthly-cycles/{cycleId}/savings-plan")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<SavingsJarContributionPlanResponse>> list(@PathVariable Long cycleId, Principal principal) {
        return ResponseEntity.ok(contributionPlanService.list(principal.getName(), cycleId));
    }

    @PostMapping("/savings-jars/{id}/planned-contributions")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<SavingsJarContributionPlanResponse> create(
            @PathVariable Long id,
            @Valid @RequestBody SavingsJarContributionPlanRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contributionPlanService.create(principal.getName(), request.cycleId(), id, request));
    }
}
