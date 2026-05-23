package br.com.kuntzedevprojects.money_master_2.dtos.finance;

public record MonthlyPlanItemReopenRequest(
        Boolean deleteLinkedTransactions,
        Boolean keepTransactionsUnlinked,
        String notes
) {
    public boolean shouldDeleteLinkedTransactions() {
        return Boolean.TRUE.equals(deleteLinkedTransactions);
    }

    public boolean shouldKeepTransactionsUnlinked() {
        return !shouldDeleteLinkedTransactions() && !Boolean.FALSE.equals(keepTransactionsUnlinked);
    }
}
