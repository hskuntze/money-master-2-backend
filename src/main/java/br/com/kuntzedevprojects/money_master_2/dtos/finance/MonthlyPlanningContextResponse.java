package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.time.Instant;
import java.util.List;

public record MonthlyPlanningContextResponse(
        FinancialPeriodResponse selectedPeriod,
        List<FinancialPeriodResponse> periods,
        MonthlyPeriodSummaryResponse summary,
        List<MonthlyPlanItemResponse> planItems,
        List<FinancialTransactionResponse> unlinkedTransactions,
        Instant generatedAt
) {
}
