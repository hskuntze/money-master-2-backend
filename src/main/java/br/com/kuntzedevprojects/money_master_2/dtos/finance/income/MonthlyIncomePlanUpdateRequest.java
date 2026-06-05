package br.com.kuntzedevprojects.money_master_2.dtos.finance.income;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.MonthlyIncomePlanStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record MonthlyIncomePlanUpdateRequest(
        Long accountId,
        Long categoryId,

        @Size(max = 255, message = "A descricao deve ter no maximo 255 caracteres.")
        String description,

        @DecimalMin(value = "0.00", message = "O valor previsto nao pode ser negativo.")
        BigDecimal expectedAmount,

        LocalDate expectedDate,
        MonthlyIncomePlanStatus status,
        Boolean recurring,
        LocalDate recurrenceEndDate,

        @Size(max = 2000, message = "As observacoes devem ter no maximo 2000 caracteres.")
        String notes
) {
}
