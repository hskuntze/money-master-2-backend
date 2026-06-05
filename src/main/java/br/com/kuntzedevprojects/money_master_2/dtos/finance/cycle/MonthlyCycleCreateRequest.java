package br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MonthlyCycleCreateRequest(
        @Size(max = 120, message = "O nome do ciclo deve ter no maximo 120 caracteres.")
        String name,

        @NotNull(message = "A data inicial do ciclo e obrigatoria.")
        LocalDate startDate,

        @NotNull(message = "A data final do ciclo e obrigatoria.")
        LocalDate endDate,

        @Min(value = 1, message = "O dia de virada deve estar entre 1 e 31.")
        @Max(value = 31, message = "O dia de virada deve estar entre 1 e 31.")
        Integer turnoverDay
) {
}
