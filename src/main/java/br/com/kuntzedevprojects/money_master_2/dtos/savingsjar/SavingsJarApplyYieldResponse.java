package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsJarApplyYieldResponse(
        Long savingsJarId,
        String savingsJarName,
        LocalDate from,
        LocalDate to,
        Integer createdYieldMovements,
        BigDecimal totalYieldApplied,
        BigDecimal currentAmount,
        String message
) {
}
