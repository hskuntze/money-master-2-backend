package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.math.BigDecimal;

import br.com.kuntzedevprojects.money_master_2.enums.FinanceCommandType;

public record FinanceCommandItem(
        FinanceCommandType type,
        String transactionType,
        BigDecimal amount,
        String description,
        String occurredOn,
        String accountName,
        String categoryName,
        String oldCategoryName,
        String newCategoryName,
        String from,
        String to,
        String savingsJarName,
        String institutionName,
        BigDecimal realYieldAmount,
        BigDecimal realCurrentAmount,
        String previousDate,
        BigDecimal targetAmount,
        String targetDate,
        String originalMessage,
        String notes
) {
}
