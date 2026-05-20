package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialSummaryResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal transferTotal,
        BigDecimal netResult,
        long transactionCount
) {
}
