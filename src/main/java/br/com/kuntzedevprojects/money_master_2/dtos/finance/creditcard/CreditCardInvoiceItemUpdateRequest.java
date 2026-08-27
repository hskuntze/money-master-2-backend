package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemConfirmationStatus;
import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemSourceType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreditCardInvoiceItemUpdateRequest(
        Long categoryId,
        @Size(max = 255) String description,
        @Positive BigDecimal amount,
        LocalDate purchaseDate,
        LocalDate competenceDate,
        CreditCardInvoiceItemSourceType sourceType,
        CreditCardInvoiceItemConfirmationStatus confirmationStatus,
        Long sourceId,
        Integer installmentNumber,
        Long transactionId,
        @Size(max = 2000) String notes
) {
}
