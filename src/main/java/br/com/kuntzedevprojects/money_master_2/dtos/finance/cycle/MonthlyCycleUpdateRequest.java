package br.com.kuntzedevprojects.money_master_2.dtos.finance.cycle;

import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record MonthlyCycleUpdateRequest(
        @Size(max = 120, message = "O nome do ciclo deve ter no maximo 120 caracteres.")
        String name,
        LocalDate startDate,
        LocalDate endDate,
        @Min(value = 1, message = "O dia de virada deve estar entre 1 e 31.")
        @Max(value = 31, message = "O dia de virada deve estar entre 1 e 31.")
        Integer turnoverDay,
        FinancialPeriodStatus status
) {
}
