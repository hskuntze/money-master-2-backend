package br.com.kuntzedevprojects.money_master_2.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FinanceCommandType {
    REGISTER_TRANSACTION,
    CHANGE_TRANSACTION_CATEGORY_BY_CATEGORY,
    CHANGE_TRANSACTION_CATEGORY_BY_DESCRIPTION_DATE,
    CREATE_CATEGORY,
    DEPOSIT_SAVINGS_JAR,
    WITHDRAW_SAVINGS_JAR,
    REGISTER_SAVINGS_JAR_YIELD,
    RECONCILE_SAVINGS_JAR_YIELD,
    RECONCILE_SAVINGS_JAR_BALANCE,
    CREATE_MONTHLY_PLAN_ITEM,
    PAY_MONTHLY_PLAN_ITEM,
    REOPEN_MONTHLY_PLAN_ITEM,
    INCREASE_MONTHLY_PLAN_ITEM_EXPECTED_AMOUNT,
    CREATE_INSTALLMENT_MONTHLY_PLAN_ITEMS,
    CREATE_INSTALLMENT_PURCHASE,
    LINK_TRANSACTION_TO_MONTHLY_PLAN_ITEM,
    RECONCILE_MONTHLY_PLAN_WITH_TRANSACTIONS,

    /**
     * Aliases kept only to tolerate occasional AI tool-call mistakes where the model
     * sends the transaction type in the command type field. They are normalized by
     * FinanceCommandExecutor to REGISTER_TRANSACTION.
     */
    INCOME,
    EXPENSE,
    TRANSFER;

    @JsonCreator
    public static FinanceCommandType fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "RECEITA", "ENTRADA", "GANHO", "SALARIO", "SALÁRIO" -> INCOME;
            case "DESPESA", "SAIDA", "SAÍDA", "GASTO", "PAGAMENTO" -> EXPENSE;
            case "TRANSFERENCIA", "TRANSFERÊNCIA" -> TRANSFER;
            default -> FinanceCommandType.valueOf(normalized);
        };
    }

    public boolean isTransactionTypeAlias() {
        return this == INCOME || this == EXPENSE || this == TRANSFER;
    }

    public String impliedTransactionType() {
        return isTransactionTypeAlias() ? name() : null;
    }
}
