package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;

import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

public record CategoryReportResponse(
        Long categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal total,
        long transactionCount
) {
}
