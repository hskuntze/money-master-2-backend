package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;

public record MonthlyPeriodSummaryResponse(
        FinancialPeriodResponse period,
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
}
