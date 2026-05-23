package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FinancialPeriodTurnoverRequest(
        @NotNull(message = "A data da virada é obrigatória.")
        LocalDate turnoverDate,

        @Size(max = 120, message = "O nome do novo mês deve ter no máximo 120 caracteres.")
        String newPeriodName
) {
}
