package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemAggregationType;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MonthlyPlanItemCreateRequest(
        Long accountId,
        Long categoryId,

        @NotNull(message = "O tipo é obrigatório.")
        TransactionType type,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "O valor previsto é obrigatório.")
        @DecimalMin(value = "0.00", message = "O valor previsto não pode ser negativo.")
        BigDecimal expectedAmount,

        @NotNull(message = "A data de vencimento/recebimento é obrigatória.")
        LocalDate dueDate,

        MonthlyPlanItemNature nature,
        MonthlyPlanItemAggregationType aggregationType,
        Long parentItemId,
        Boolean recurring,
        LocalDate recurrenceEndDate,
        MonthlyPlanItemStatus status,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes
) {
}
