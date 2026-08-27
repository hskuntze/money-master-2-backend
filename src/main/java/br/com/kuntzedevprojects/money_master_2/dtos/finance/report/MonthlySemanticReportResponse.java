package br.com.kuntzedevprojects.money_master_2.dtos.finance.report;

import java.math.BigDecimal;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryReportResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.DailyCashFlowResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle.MonthlyCycleResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.dashboard.MonthlyDashboardAlertResponse;

public record MonthlySemanticReportResponse(
        MonthlyCycleResponse cycle,
        PlanningSection planning,
        RealizedSection realized,
        CreditCardSection creditCards,
        InstallmentSection installments,
        SavingsJarSection savingsJars,
        InvestmentSection investments,
        BigDecimal cashBalanceCurrent,
        BigDecimal projectedAvailableAmount,
        List<DailyCashFlowResponse> cashFlow,
        List<CategoryReportResponse> expenseCategories,
        List<CategoryReportResponse> incomeCategories,
        List<MonthlyDashboardAlertResponse> alerts
) {
    public record PlanningSection(
            BigDecimal plannedIncomeTotal,
            BigDecimal plannedPayablesTotal,
            BigDecimal plannedCreditCardInvoicesTotal,
            BigDecimal plannedSavingsTotal,
            BigDecimal plannedInvestmentTotal,
            BigDecimal plannedAvailableAmount
    ) {
    }

    public record RealizedSection(
            BigDecimal receivedIncomeTotal,
            BigDecimal paidPayablesTotal,
            BigDecimal paidCreditCardInvoicesTotal,
            BigDecimal actualSavingsTotal,
            BigDecimal actualInvestmentTotal,
            BigDecimal unplannedIncomeTotal,
            BigDecimal unplannedExpenseTotal,
            BigDecimal realizedAvailableAmount
    ) {
    }

    public record CreditCardSection(
            BigDecimal invoicesTotal,
            BigDecimal invoicesPaidTotal,
            BigDecimal invoicesPendingTotal
    ) {
    }

    public record InstallmentSection(
            BigDecimal currentMonthTotal,
            BigDecimal futureTotal,
            BigDecimal anticipatedTotal
    ) {
    }

    public record SavingsJarSection(
            BigDecimal totalSaved,
            BigDecimal totalTarget,
            BigDecimal monthlyPlannedContribution,
            BigDecimal monthlyActualContribution,
            BigDecimal totalYield,
            BigDecimal averageProgressPercentage
    ) {
    }

    public record InvestmentSection(
            BigDecimal totalAmount,
            BigDecimal totalContributed,
            BigDecimal totalWithdrawn,
            BigDecimal totalYield,
            BigDecimal monthlyPlannedContribution,
            BigDecimal monthlyActualContribution,
            long activeProductCount
    ) {
    }
}
