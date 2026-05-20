package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;

public record ComparativeReportResponse(
        FinancialSummaryResponse firstPeriod,
        FinancialSummaryResponse secondPeriod,
        BigDecimal incomeDifference,
        BigDecimal expenseDifference,
        BigDecimal netDifference
) {
}
