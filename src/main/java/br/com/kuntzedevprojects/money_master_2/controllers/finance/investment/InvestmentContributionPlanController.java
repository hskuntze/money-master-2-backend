package br.com.kuntzedevprojects.money_master_2.controllers.finance.investment;

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

import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentContributionPlanRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.investment.InvestmentContributionPlanResponse;
import br.com.kuntzedevprojects.money_master_2.services.finance.investment.InvestmentContributionPlanService;
import jakarta.validation.Valid;

@RestController
public class InvestmentContributionPlanController {

    private final InvestmentContributionPlanService contributionPlanService;

    public InvestmentContributionPlanController(InvestmentContributionPlanService contributionPlanService) {
        this.contributionPlanService = contributionPlanService;
    }

    @GetMapping("/monthly-cycles/{cycleId}/investment-plan")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<InvestmentContributionPlanResponse>> list(@PathVariable Long cycleId, Principal principal) {
        return ResponseEntity.ok(contributionPlanService.list(principal.getName(), cycleId));
    }

    @PostMapping("/investments/{id}/planned-contributions")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<InvestmentContributionPlanResponse> create(
            @PathVariable Long id,
            @Valid @RequestBody InvestmentContributionPlanRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contributionPlanService.create(principal.getName(), request.cycleId(), id, request));
    }
}
