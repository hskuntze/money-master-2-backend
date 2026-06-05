package br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MonthlyCycleTurnoverRequest(
        @NotNull(message = "A data da virada e obrigatoria.")
        LocalDate turnoverDate,

        @Size(max = 120, message = "O nome do novo ciclo deve ter no maximo 120 caracteres.")
        String newCycleName
) {
}
