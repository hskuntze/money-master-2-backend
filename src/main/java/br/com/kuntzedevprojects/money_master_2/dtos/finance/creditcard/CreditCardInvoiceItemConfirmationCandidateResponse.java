package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.util.List;

import br.com.kuntzedevprojects.money_master_2.dtos.finance.FinancialTransactionResponse;

public record CreditCardInvoiceItemConfirmationCandidateResponse(
        FinancialTransactionResponse transaction,
        int score,
        boolean amountMatches,
        boolean dateMatches,
        boolean categoryMatches,
        List<String> reasons
) {
}
