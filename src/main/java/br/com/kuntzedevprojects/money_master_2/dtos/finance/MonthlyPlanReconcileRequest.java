package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;

public record MonthlyPlanReconcileRequest(
        Boolean dryRun,
        Boolean createMissingPlanItems,
        Boolean linkExistingTransactions,
        Boolean onlyUnlinkedTransactions,
        MonthlyPlanItemNature defaultNature,
        Boolean recurring,
        TransactionType type,
        LocalDate from,
        LocalDate to,
        Boolean createPlanItemsAsPendingOnly,
        Boolean deleteSourceTransactionsWhenCreatingPlanItems
) {
    public boolean isDryRun() {
        return dryRun == null || dryRun;
    }

    public boolean shouldCreateMissingPlanItems() {
        return Boolean.TRUE.equals(createMissingPlanItems);
    }

    public boolean shouldLinkExistingTransactions() {
        return linkExistingTransactions == null || linkExistingTransactions;
    }

    public boolean shouldOnlyUnlinkedTransactions() {
        return onlyUnlinkedTransactions == null || onlyUnlinkedTransactions;
    }

    public boolean shouldCreatePlanItemsAsPendingOnly() {
        return Boolean.TRUE.equals(createPlanItemsAsPendingOnly);
    }

    public boolean shouldDeleteSourceTransactionsWhenCreatingPlanItems() {
        return Boolean.TRUE.equals(deleteSourceTransactionsWhenCreatingPlanItems);
    }
}
