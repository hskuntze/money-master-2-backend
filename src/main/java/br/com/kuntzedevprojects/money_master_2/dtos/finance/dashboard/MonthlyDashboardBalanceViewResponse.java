package br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyDashboardBalanceViewResponse(
        String key,
        String label,
        String description,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal pendingIncomeTotal,
        BigDecimal pendingExpenseTotal,
        BigDecimal availableAmount,
        List<MonthlyDashboardBalanceLineResponse> lines
) {
}
