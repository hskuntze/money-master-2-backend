package br.com.kuntzedevprojects.money_master_2.dtos.finance.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.CreditCardInvoiceStatus;
import jakarta.validation.constraints.PositiveOrZero;

public record CreditCardInvoiceUpdateRequest(
        LocalDate openingDate,
        LocalDate closingDate,
        LocalDate dueDate,
        CreditCardInvoiceStatus status,
        @PositiveOrZero BigDecimal finalAmount
) {
}
