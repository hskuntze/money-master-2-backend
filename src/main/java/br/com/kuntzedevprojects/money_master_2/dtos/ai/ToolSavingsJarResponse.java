package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ToolSavingsJarResponse(
        Long id,
        String name,
        String institutionName,
        BigDecimal currentAmount,
        BigDecimal targetAmount,
        BigDecimal progressPercentage,
        BigDecimal totalYield,
        BigDecimal projectedYieldToday,
        LocalDate projectedYieldReferenceDate,
        String message
) {
}
