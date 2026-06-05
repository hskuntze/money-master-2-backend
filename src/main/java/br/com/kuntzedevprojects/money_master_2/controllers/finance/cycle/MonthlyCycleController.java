package br.com.kuntzedevprojects.money_master_2.controllers.finance.cycle;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleCreateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleTurnoverRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleUpdateRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardResponse;
import br.com.kuntzedevprojects.money_master_2.services.finance.cycle.MonthlyCycleClosingService;
import br.com.kuntzedevprojects.money_master_2.services.finance.cycle.MonthlyCycleQueryService;
import br.com.kuntzedevprojects.money_master_2.services.finance.cycle.MonthlyCycleService;
import br.com.kuntzedevprojects.money_master_2.services.finance.dashboard.MonthlyDashboardService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/monthly-cycles")
public class MonthlyCycleController {

    private final MonthlyCycleQueryService queryService;
    private final MonthlyCycleService cycleService;
    private final MonthlyCycleClosingService closingService;
    private final MonthlyDashboardService dashboardService;

    public MonthlyCycleController(
            MonthlyCycleQueryService queryService,
            MonthlyCycleService cycleService,
            MonthlyCycleClosingService closingService,
            MonthlyDashboardService dashboardService
    ) {
        this.queryService = queryService;
        this.cycleService = cycleService;
        this.closingService = closingService;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<MonthlyCycleResponse>> list(Principal principal) {
        return ResponseEntity.ok(queryService.list(principal.getName()));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<MonthlyCycleResponse> current(Principal principal) {
        return ResponseEntity.ok(queryService.current(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<MonthlyCycleResponse> get(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(queryService.get(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyCycleResponse> create(
            @Valid @RequestBody MonthlyCycleCreateRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cycleService.create(principal.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyCycleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MonthlyCycleUpdateRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(cycleService.update(principal.getName(), id, request));
    }

    @PostMapping("/turnover")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyCycleResponse> turnover(
            @Valid @RequestBody MonthlyCycleTurnoverRequest request,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cycleService.turnover(principal.getName(), request));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyCycleResponse> close(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(closingService.close(principal.getName(), id));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    public ResponseEntity<MonthlyCycleResponse> reopen(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(closingService.reopen(principal.getName(), id));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<MonthlyCycleSummaryResponse> summary(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(queryService.summary(principal.getName(), id));
    }

    @GetMapping("/{id}/dashboard")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<MonthlyDashboardResponse> dashboard(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(dashboardService.get(principal.getName(), id));
    }
}
