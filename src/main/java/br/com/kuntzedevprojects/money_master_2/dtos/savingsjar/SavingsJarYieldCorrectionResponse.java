package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsJarYieldCorrectionResponse(
        Long savingsJarId,
        String savingsJarName,
        LocalDate occurredOn,
        BigDecimal previousYieldAmount,
        BigDecimal realYieldAmount,
        BigDecimal adjustmentAmount,
        BigDecimal currentAmountAfterAdjustment,
        SavingsJarMovementResponse movement,
        String message
) {
}
