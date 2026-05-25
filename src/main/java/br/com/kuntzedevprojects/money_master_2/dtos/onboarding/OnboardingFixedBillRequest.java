package br.com.kuntzedevprojects.money_master_2.dtos.onboarding;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record OnboardingFixedBillRequest(
        @Size(max = 255, message = "O nome da conta fixa deve ter no máximo 255 caracteres.")
        String description,

        @DecimalMin(value = "0.01", message = "O valor da conta fixa deve ser maior que zero.")
        BigDecimal amount,

        @Min(value = 1, message = "O vencimento deve estar entre 1 e 31.")
        @Max(value = 31, message = "O vencimento deve estar entre 1 e 31.")
        Integer dueDay
) {
}
