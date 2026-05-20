package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record FinancialTransactionUpdateRequest(
        Long accountId,
        Long categoryId,
        TransactionType type,
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,
        LocalDate occurredOn,
        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes
) {
}
