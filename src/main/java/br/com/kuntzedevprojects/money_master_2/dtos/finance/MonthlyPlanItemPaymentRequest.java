package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record MonthlyPlanItemPaymentRequest(
        Long transactionId,
        Long accountId,
        Long categoryId,
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,
        LocalDate occurredOn,
        Boolean preferExistingTransaction,
        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes
) {
}
