package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.time.LocalDate;

public record ToolTransactionCategoryUpdateResponse(
        int updatedCount,
        String oldCategoryName,
        String newCategoryName,
        String transactionType,
        LocalDate from,
        LocalDate to,
        String message
) {
}
