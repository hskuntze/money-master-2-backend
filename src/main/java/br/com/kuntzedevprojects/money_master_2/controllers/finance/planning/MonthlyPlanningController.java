package br.com.kuntzedevprojects.money_master_2.controllers.finance.planning;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.services.finance.planning.MonthlyPlanningService;
import br.com.kuntzedevprojects.money_master_2.services.finance.planning.MonthlyPlanningService.MonthlyPlanningResponse;

@RestController
public class MonthlyPlanningController {

    private final MonthlyPlanningService planningService;

    public MonthlyPlanningController(MonthlyPlanningService planningService) {
        this.planningService = planningService;
    }

    @GetMapping("/monthly-cycles/{cycleId}/planning")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<MonthlyPlanningResponse> getPlanning(@PathVariable Long cycleId, Principal principal) {
        return ResponseEntity.ok(planningService.getPlanning(principal.getName(), cycleId));
    }
}
