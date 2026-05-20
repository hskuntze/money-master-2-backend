package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.util.List;

public record SavingsJarSummaryResponse(
        BigDecimal totalSaved,
        BigDecimal totalTarget,
        BigDecimal totalYield,
        BigDecimal remainingToTargets,
        BigDecimal averageProgressPercentage,
        Integer totalJars,
        List<SavingsJarResponse> jars
) {
}
