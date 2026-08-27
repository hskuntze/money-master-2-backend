package br.com.kuntzedevprojects.money_master_2.dtos.investment;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record InvestmentProductUpdateRequest(
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name,

        @Size(max = 80, message = "O tipo deve ter no máximo 80 caracteres.")
        String typeName,

        @Size(max = 120, message = "A instituição deve ter no máximo 120 caracteres.")
        String institutionName,

        Long linkedAccountId,
        Boolean removeLinkedAccount,

        @Size(max = 120, message = "A liquidez deve ter no máximo 120 caracteres.")
        String liquidity,

        @DecimalMin(value = "0.00", message = "O saldo atual deve ser maior ou igual a zero.")
        BigDecimal currentAmount,

        Boolean active,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String notes
) {
}
