package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreditCardInvoiceCreateRequest(
        @NotNull Long cycleId,
        @NotNull @Min(1) @Max(12) Integer referenceMonth,
        @NotNull @Min(1900) Integer referenceYear,
        LocalDate openingDate,
        LocalDate closingDate,
        LocalDate dueDate
) {
}
