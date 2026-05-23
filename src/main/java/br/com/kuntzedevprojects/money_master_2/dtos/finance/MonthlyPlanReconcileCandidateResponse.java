package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyPlanReconcileCandidateResponse(
        Long transactionId,
        String transactionDescription,
        BigDecimal transactionAmount,
        LocalDate transactionDate,
        Long planItemId,
        String planItemDescription,
        BigDecimal expectedAmount,
        LocalDate dueDate,
        int score,
        String action,
        boolean executed,
        String message
) {
}
