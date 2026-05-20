package br.com.kuntzedevprojects.money_master_2.dtos.savingsjar;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.kuntzedevprojects.money_master_2.enums.SavingsJarYieldCalculationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record SavingsJarUpdateRequest(
        @Size(max = 120, message = "O nome do cofrinho deve ter no máximo 120 caracteres.")
        String name,

        @Size(max = 120, message = "O nome da instituição deve ter no máximo 120 caracteres.")
        String institutionName,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @DecimalMin(value = "0.00", message = "A meta deve ser maior ou igual a zero.")
        BigDecimal targetAmount,

        LocalDate targetDate,

        @Size(max = 500, message = "A URL da imagem deve ter no máximo 500 caracteres.")
        String imageUrl,

        @Size(max = 50, message = "O ícone deve ter no máximo 50 caracteres.")
        String icon,

        @Size(max = 20, message = "A cor deve ter no máximo 20 caracteres.")
        String color,

        Long linkedAccountId,
        Boolean removeLinkedAccount,
        Boolean active,
        Boolean yieldEnabled,
        SavingsJarYieldCalculationType yieldCalculationType,

        @DecimalMin(value = "0.00", message = "O percentual de rendimento deve ser maior ou igual a zero.")
        BigDecimal yieldPercentage,

        Boolean businessDaysOnly,
        Boolean useBrazilianHolidays,
        LocalDate yieldStartDate,
        LocalDate lastYieldCalculationDate
) {
}
