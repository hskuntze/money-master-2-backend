package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FinancialTransactionCreateRequest(
        @NotNull(message = "A conta é obrigatória.")
        Long accountId,

        Long categoryId,

        Long financialPeriodId,

        Long monthlyPlanItemId,

        @NotNull(message = "O tipo do lançamento é obrigatório.")
        TransactionType type,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @NotNull(message = "A data do lançamento é obrigatória.")
        LocalDate occurredOn,

        TransactionSource source,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes
) {
}
