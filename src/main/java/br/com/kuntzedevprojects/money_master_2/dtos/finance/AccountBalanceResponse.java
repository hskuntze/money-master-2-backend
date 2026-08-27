package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;

import br.com.kuntzedevprojects.money_master_2.enums.AccountType;

public record AccountBalanceResponse(
        Long accountId,
        String accountName,
        AccountType accountType,
        BigDecimal initialBalance,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal transferTotal,
        BigDecimal currentBalance,
        BigDecimal reservedInSavingsJars,
        BigDecimal availableBalance
) {
}
