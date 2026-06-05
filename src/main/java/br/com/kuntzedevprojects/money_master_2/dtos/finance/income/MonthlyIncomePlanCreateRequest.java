package br.com.kuntzedevprojects.money_master_2.dtos.finance.income;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.MonthlyIncomePlanStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MonthlyIncomePlanCreateRequest(
        Long accountId,
        Long categoryId,

        @NotBlank(message = "A descricao da receita e obrigatoria.")
        @Size(max = 255, message = "A descricao deve ter no maximo 255 caracteres.")
        String description,

        @NotNull(message = "O valor previsto e obrigatorio.")
        @DecimalMin(value = "0.00", message = "O valor previsto nao pode ser negativo.")
        BigDecimal expectedAmount,

        @NotNull(message = "A data prevista de recebimento e obrigatoria.")
        LocalDate expectedDate,

        MonthlyIncomePlanStatus status,
        Boolean recurring,
        LocalDate recurrenceEndDate,

        @Size(max = 2000, message = "As observacoes devem ter no maximo 2000 caracteres.")
        String notes
) {
}
