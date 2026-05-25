package br.com.kuntzedevprojects.money_master_2.dtos.profile;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UserFinancialProfileRequest(
        @Min(value = 0, message = "A idade não pode ser negativa.")
        @Max(value = 120, message = "A idade informada parece inválida.")
        Integer age,

        @Size(max = 80)
        String ageRange,

        @Size(max = 160)
        String profession,

        @Size(max = 80)
        String preferredName,

        @Min(value = 1, message = "O dia de início do ciclo deve estar entre 1 e 31.")
        @Max(value = 31, message = "O dia de início do ciclo deve estar entre 1 e 31.")
        Integer cycleStartDay,

        @Min(value = 1, message = "O dia de recebimento deve estar entre 1 e 31.")
        @Max(value = 31, message = "O dia de recebimento deve estar entre 1 e 31.")
        Integer incomeDay,

        @DecimalMin(value = "0.00", message = "A renda aproximada não pode ser negativa.")
        BigDecimal approximateMonthlyIncome,

        @DecimalMin(value = "0.00", message = "O valor da meta inicial não pode ser negativo.")
        BigDecimal initialGoalTargetAmount,

        @Size(max = 40)
        String onboardingVersion,

        @Size(max = 1000)
        String currentFinancialSituation,

        @Size(max = 1000)
        String spendingHabits,

        @Size(max = 1000)
        String financialObjectives,

        @Size(max = 1000)
        String shortTermGoals,

        @Size(max = 1000)
        String mediumTermGoals,

        @Size(max = 1000)
        String longTermGoals,

        @Size(max = 80)
        String riskTolerance,

        @Size(max = 80)
        String investmentKnowledge,

        @Size(max = 80)
        String investorProfile,

        @Size(max = 1000)
        String financialPreferences,

        Boolean onboardingCompleted,

        Boolean tourCompleted,

        Boolean tourSkipped,

        @Size(max = 120)
        String tourLastStepKey
) {
}
