package br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle;

import java.math.BigDecimal;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.MonthlyPeriodSummaryResponse;

public record MonthlyCycleSummaryResponse(
        MonthlyCycleResponse cycle,
        BigDecimal plannedIncomeTotal,
        BigDecimal plannedExpenseTotal,
        BigDecimal paidIncomeTotal,
        BigDecimal paidExpenseTotal,
        BigDecimal pendingIncomeTotal,
        BigDecimal pendingExpenseTotal,
        BigDecimal realizedIncomeTotal,
        BigDecimal realizedExpenseTotal,
        BigDecimal plannedAvailableAmount,
        BigDecimal unplannedIncomeTotal,
        BigDecimal unplannedExpenseTotal,
        BigDecimal projectedAvailableAmount,
        long pendingItems,
        long paidItems
) {
    public static MonthlyCycleSummaryResponse from(MonthlyPeriodSummaryResponse summary) {
        return new MonthlyCycleSummaryResponse(
                MonthlyCycleResponse.from(summary.period()),
                summary.plannedIncomeTotal(),
                summary.plannedExpenseTotal(),
                summary.paidIncomeTotal(),
                summary.paidExpenseTotal(),
                summary.pendingIncomeTotal(),
                summary.pendingExpenseTotal(),
                summary.realizedIncomeTotal(),
                summary.realizedExpenseTotal(),
                summary.plannedAvailableAmount(),
                summary.unplannedIncomeTotal(),
                summary.unplannedExpenseTotal(),
                summary.projectedAvailableAmount(),
                summary.pendingItems(),
                summary.paidItems()
        );
    }
}
