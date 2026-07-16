package br.com.kuntzedevprojects.money_master_2.dtos.finance.debt;

import java.math.BigDecimal;

public record DebtSummaryResponse(
        BigDecimal totalOriginalPrincipal,
        BigDecimal totalOutstandingPrincipal,
        BigDecimal totalOutstandingScheduled,
        BigDecimal totalMonthlyCommitment,
        Integer activeDebts,
        Integer overdueInstallments
) {
}
