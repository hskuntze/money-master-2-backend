package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ToolTransactionResponse(
        Long transactionId,
        String type,
        String description,
        BigDecimal amount,
        LocalDate occurredOn,
        String accountName,
        String categoryName,
        BigDecimal accountCurrentBalance,
        String message
) {
}
