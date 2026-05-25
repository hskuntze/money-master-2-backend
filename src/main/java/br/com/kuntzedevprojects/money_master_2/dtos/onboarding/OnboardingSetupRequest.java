package br.com.kuntzedevprojects.money_master_2.dtos.onboarding;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OnboardingSetupRequest(
        @Size(max = 80, message = "O apelido deve ter no máximo 80 caracteres.")
        String preferredName,

        @Min(value = 0, message = "A idade não pode ser negativa.")
        @Max(value = 120, message = "A idade informada parece inválida.")
        Integer age,

        @Size(max = 160, message = "A profissão deve ter no máximo 160 caracteres.")
        String profession,

        @NotNull(message = "O dia de início do ciclo mensal é obrigatório.")
        @Min(value = 1, message = "O dia de início do ciclo deve estar entre 1 e 31.")
        @Max(value = 31, message = "O dia de início do ciclo deve estar entre 1 e 31.")
        Integer cycleStartDay,

        @NotNull(message = "O valor da renda mensal principal é obrigatório.")
        @DecimalMin(value = "0.00", message = "A renda mensal não pode ser negativa.")
        BigDecimal monthlyIncome,

        @NotNull(message = "O dia de recebimento é obrigatório.")
        @Min(value = 1, message = "O dia de recebimento deve estar entre 1 e 31.")
        @Max(value = 31, message = "O dia de recebimento deve estar entre 1 e 31.")
        Integer incomeDay,

        @Valid
        List<OnboardingFixedBillRequest> fixedBills,

        @Size(max = 8, message = "Selecione no máximo 8 objetivos.")
        List<@Size(max = 80) String> goals,

        @DecimalMin(value = "0.00", message = "O valor da meta inicial não pode ser negativo.")
        BigDecimal initialGoalTargetAmount,

        @Size(max = 80)
        String investmentKnowledge,

        @Size(max = 80)
        String riskTolerance,

        @Size(max = 80)
        String investorProfile,

        Boolean startTourAfterOnboarding
) {
}
