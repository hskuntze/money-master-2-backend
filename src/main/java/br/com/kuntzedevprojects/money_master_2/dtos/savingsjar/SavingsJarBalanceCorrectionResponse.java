package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsJarBalanceCorrectionResponse(
        Long savingsJarId,
        String savingsJarName,
        String institutionName,
        LocalDate occurredOn,
        LocalDate previousDate,
        BigDecimal previousAmount,
        BigDecimal realCurrentAmount,
        BigDecimal systemCurrentAmountBeforeCorrection,
        BigDecimal adjustmentAmount,
        BigDecimal periodYieldAmount,
        BigDecimal currentAmountAfterCorrection,
        SavingsJarMovementResponse movement,
        String message
) {
}
