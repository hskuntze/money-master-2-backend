package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import jakarta.validation.constraints.NotNull;

public record MonthlyPlanItemLinkTransactionRequest(
        @NotNull(message = "A transação é obrigatória.")
        Long transactionId,
        Boolean copyCategoryFromPlanItem,
        Boolean copyAccountFromPlanItem,
        Boolean forceRelink
) {
}
