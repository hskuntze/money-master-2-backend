package br.com.kuntzedevprojects.money_master_2.dtos.finance;

public record MonthlyPlanItemUnlinkTransactionRequest(
        Long transactionId,
        Boolean deleteTransaction,
        String notes
) {
    public boolean shouldDeleteTransaction() {
        return Boolean.TRUE.equals(deleteTransaction);
    }
}
