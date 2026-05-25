package br.com.kuntzedevprojects.money_master_2.dtos.installments;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InstallmentPurchaseCreateRequest(
        @NotBlank(message = "A descrição da compra é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @DecimalMin(value = "0.01", message = "O valor total deve ser maior que zero.")
        BigDecimal totalAmount,

        @NotNull(message = "A quantidade de parcelas é obrigatória.")
        @Min(value = 1, message = "A quantidade mínima é 1 parcela.")
        @Max(value = 120, message = "A quantidade máxima é 120 parcelas.")
        Integer installmentCount,

        @DecimalMin(value = "0.01", message = "O valor da parcela deve ser maior que zero.")
        BigDecimal installmentAmount,

        LocalDate purchaseDate,

        @NotNull(message = "A data da primeira parcela é obrigatória.")
        LocalDate firstDueDate,

        Long categoryId,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes
) {
}
