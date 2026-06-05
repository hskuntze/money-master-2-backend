package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceItemSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreditCardInvoiceItemCreateRequest(
        Long categoryId,
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate purchaseDate,
        LocalDate competenceDate,
        CreditCardInvoiceItemSourceType sourceType,
        Long sourceId,
        Integer installmentNumber,
        Long transactionId,
        @Size(max = 2000) String notes
) {
}
