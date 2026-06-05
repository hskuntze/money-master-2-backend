package br.com.kuntzedevprojects.money_master_2.controllers;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryReportResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.ComparativeReportResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.DailyCashFlowResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialSummaryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.report.MonthlySemanticReportResponse;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import br.com.kuntzedevprojects.money_master_2.services.FinancialReportService;

@RestController
@RequestMapping("/reports")
public class FinancialReportController {

    private final FinancialReportService reportService;

    public FinancialReportController(FinancialReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<FinancialSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal
    ) {
        return ResponseEntity.ok(reportService.summary(principal.getName(), from, to));
    }

    @GetMapping("/accounts/balances")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<AccountBalanceResponse>> accountBalances(Principal principal) {
        return ResponseEntity.ok(reportService.accountBalances(principal.getName()));
    }

    @GetMapping("/cash-flow/daily")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<DailyCashFlowResponse>> dailyCashFlow(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal
    ) {
        return ResponseEntity.ok(reportService.dailyCashFlow(principal.getName(), from, to));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<List<CategoryReportResponse>> byCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            Principal principal
    ) {
        return ResponseEntity.ok(reportService.byCategory(principal.getName(), from, to, type));
    }

    @GetMapping("/compare")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<ComparativeReportResponse> compare(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromA,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toA,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toB,
            Principal principal
    ) {
        return ResponseEntity.ok(reportService.compare(principal.getName(), fromA, toA, fromB, toB));
    }

    @GetMapping("/monthly-semantic")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<MonthlySemanticReportResponse> monthlySemantic(
            @RequestParam Long cycleId,
            Principal principal
    ) {
        return ResponseEntity.ok(reportService.monthlySemantic(principal.getName(), cycleId));
    }
}
