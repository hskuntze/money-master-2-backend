package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AiPrivacySettingsRequest(
        Boolean aiEnabled,
        Boolean consentGranted,
        Boolean shareFinancialProfile,
        Boolean shareMonthlySummary,
        Boolean shareRecentTransactions,
        Boolean shareSavingsGoals,
        Boolean allowWriteOperations,
        Boolean maskSensitiveValues,
        @Min(1)
        @Max(365)
        Integer retentionDays
) {
}
