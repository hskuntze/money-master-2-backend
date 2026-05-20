package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.TransactionSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavingsJarMovementRequest(
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        LocalDate occurredOn,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        TransactionSource source,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes
) {
}
