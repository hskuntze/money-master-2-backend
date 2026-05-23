package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.FinancialPeriodStatus;
import jakarta.validation.constraints.Size;

public record FinancialPeriodUpdateRequest(
        @Size(max = 120, message = "O nome do ciclo deve ter no máximo 120 caracteres.")
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Integer turnoverDay,
        FinancialPeriodStatus status
) {
}
