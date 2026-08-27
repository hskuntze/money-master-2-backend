package br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard;

import java.math.BigDecimal;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;

public record MonthlyDashboardResponse(
        MonthlyCycleResponse cycle,
        BigDecimal plannedIncomeTotal,
        BigDecimal receivedIncomeTotal,
        BigDecimal pendingIncomeTotal,
        BigDecimal plannedPayablesTotal,
        BigDecimal paidPayablesTotal,
        BigDecimal pendingPayablesTotal,
        BigDecimal creditCardInvoicesTotal,
        BigDecimal creditCardInvoicesPaidTotal,
        BigDecimal creditCardInvoicesPendingTotal,
        BigDecimal installmentsCurrentMonthTotal,
        BigDecimal installmentsFutureTotal,
        BigDecimal anticipatedInstallmentsTotal,
        BigDecimal savingsPlannedTotal,
        BigDecimal savingsActualTotal,
        BigDecimal investmentPlannedTotal,
        BigDecimal investmentActualTotal,
        BigDecimal unplannedIncomeTotal,
        BigDecimal unplannedExpenseTotal,
        BigDecimal cashBalanceCurrent,
        BigDecimal plannedAvailableAmount,
        BigDecimal realizedAvailableAmount,
        BigDecimal projectedAvailableAmount,
        MonthlyDashboardBalanceOverviewResponse balanceOverview,
        List<MonthlyDashboardAlertResponse> alerts
) {
}
