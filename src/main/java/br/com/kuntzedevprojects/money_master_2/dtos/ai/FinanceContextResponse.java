package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountBalanceResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.AccountResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.CategoryResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.savingsjar.SavingsJarResponse;

public record FinanceContextResponse(
        LocalDate from,
        LocalDate to,
        List<AccountResponse> accounts,
        List<AccountBalanceResponse> accountBalances,
        List<CategoryResponse> categories,
        List<SavingsJarResponse> savingsJars,
        List<FinancialTransactionResponse> recentTransactions,
        Instant generatedAt
) {
}
