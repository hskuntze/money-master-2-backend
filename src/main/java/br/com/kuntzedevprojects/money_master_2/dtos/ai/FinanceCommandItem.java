package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.math.BigDecimal;

import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

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
        Long financialPeriodId,
        Long monthlyPlanItemId,
        Long transactionId,
        String planItemDescription,
        String transactionDescription,
        String dueDate,
        String planItemNature,
        Boolean recurring,
        Boolean createIfMissing,
        Boolean createMissingPlanItems,
        Boolean linkExistingTransactions,
        Boolean onlyUnlinkedTransactions,
        Boolean preferExistingTransaction,
        Boolean forceRelink,
        Boolean deleteLinkedTransactions,
        Boolean createPlanItemsAsPendingOnly,
        Boolean deleteSourceTransactionsWhenCreatingPlanItems,
        Boolean skipMonthlyPlanAutoAdjustment,
        Integer installmentCount,
        Integer installmentsToPay,
        Integer targetPaidInstallments,
        Long installmentPurchaseId,
        String installmentPurchaseDescription,
        String paymentDate,
        String firstDueDate,
        String recurrenceEndDate,
        TransactionType reconcileTransactionType,
        MonthlyPlanItemNature defaultNature,
        String originalMessage,
        String notes
) {
}
