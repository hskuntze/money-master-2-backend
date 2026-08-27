package br.com.kuntzedevprojects.money_master_2.dtos.investment;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.payable.MonthlyPayableResponse;

public record InvestmentContributionPlanResponse(
        Long investmentProductId,
        String investmentProductName,
        MonthlyPayableResponse payable
) {
}
