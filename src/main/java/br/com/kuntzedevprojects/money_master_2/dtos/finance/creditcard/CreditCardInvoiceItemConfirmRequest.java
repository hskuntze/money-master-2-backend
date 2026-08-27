package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreditCardInvoiceItemConfirmRequest(
        Long transactionId,
        @Positive BigDecimal amount,
        LocalDate purchaseDate,
        Long categoryId,
        @Size(max = 2000) String notes
) {
}
