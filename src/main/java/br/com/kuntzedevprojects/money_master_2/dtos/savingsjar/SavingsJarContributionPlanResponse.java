package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableResponse;

public record SavingsJarContributionPlanResponse(
        Long savingsJarId,
        String savingsJarName,
        MonthlyPayableResponse payable
) {
}
