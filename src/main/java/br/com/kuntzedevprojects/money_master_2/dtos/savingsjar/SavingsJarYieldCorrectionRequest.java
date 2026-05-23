package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavingsJarYieldCorrectionRequest(
        @NotNull(message = "O rendimento real é obrigatório.")
        @DecimalMin(value = "0.00", message = "O rendimento real não pode ser negativo.")
        BigDecimal realYieldAmount,

        LocalDate occurredOn,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes
) {
}
