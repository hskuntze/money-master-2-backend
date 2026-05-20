package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsJarYieldPreviewResponse(
        LocalDate referenceDate,
        BigDecimal baseAmount,
        BigDecimal cdiDailyRate,
        BigDecimal appliedRate,
        BigDecimal projectedYieldAmount,
        boolean estimated,
        String message
) {
}
