package br.com.kuntzedevprojects.money_master_2.dtos.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemNature;
import br.com.kuntzedevprojects.money_master_2.enums.MonthlyPlanItemStatus;
import br.com.kuntzedevprojects.money_master_2.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record MonthlyPlanItemUpdateRequest(
        Long accountId,
        Long categoryId,
        TransactionType type,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @DecimalMin(value = "0.00", message = "O valor previsto não pode ser negativo.")
        BigDecimal expectedAmount,

        @DecimalMin(value = "0.00", message = "O valor realizado não pode ser negativo.")
        BigDecimal actualAmount,

        LocalDate dueDate,
        LocalDate paidOn,
        MonthlyPlanItemNature nature,
        Boolean recurring,
        LocalDate recurrenceEndDate,
        MonthlyPlanItemStatus status,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes
) {
}
