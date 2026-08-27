package br.com.kuntzedevprojects.money_master_2.dtos.ai;

import java.time.Instant;

import br.com.kuntzedevprojects.money_master_2.entities.AiPrivacySettings;

public record AiPrivacySettingsResponse(
        Long id,
        boolean aiEnabled,
        boolean consentGranted,
        boolean shareFinancialProfile,
        boolean shareMonthlySummary,
        boolean shareRecentTransactions,
        boolean shareSavingsGoals,
        boolean shareInvestmentProducts,
        boolean allowWriteOperations,
        boolean maskSensitiveValues,
        Integer retentionDays,
        Instant consentGrantedAt,
        Instant consentRevokedAt,
        Instant updatedAt
) {
    public static AiPrivacySettingsResponse from(AiPrivacySettings settings) {
        return new AiPrivacySettingsResponse(
                settings.getId(),
                settings.isAiEnabled(),
                settings.isConsentGranted(),
                settings.isShareFinancialProfile(),
                settings.isShareMonthlySummary(),
                settings.isShareRecentTransactions(),
                settings.isShareSavingsGoals(),
                settings.isShareInvestmentProducts(),
                settings.isAllowWriteOperations(),
                settings.isMaskSensitiveValues(),
                settings.getRetentionDays(),
                settings.getConsentGrantedAt(),
                settings.getConsentRevokedAt(),
                settings.getUpdatedAt()
        );
    }
}
