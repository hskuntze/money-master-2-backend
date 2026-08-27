package br.com.kuntzedevprojects.money_master_2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiPrivacySettingsRequest;
import br.com.kuntzedevprojects.money_master_2.dtos.ai.AiPrivacySettingsResponse;
import br.com.kuntzedevprojects.money_master_2.entities.AiPrivacySettings;
import br.com.kuntzedevprojects.money_master_2.entities.User;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.AiChatConversationRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.AiChatMessageRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandAuditRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.AiCommandConfirmationRepository;
import br.com.kuntzedevprojects.money_master_2.repositories.AiPrivacySettingsRepository;

class AiPrivacySettingsServiceTest {

    private final AiPrivacySettingsRepository settingsRepository = mock(AiPrivacySettingsRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AiChatConversationRepository conversationRepository = mock(AiChatConversationRepository.class);
    private final AiChatMessageRepository messageRepository = mock(AiChatMessageRepository.class);
    private final AiCommandAuditRepository auditRepository = mock(AiCommandAuditRepository.class);
    private final AiCommandConfirmationRepository confirmationRepository = mock(AiCommandConfirmationRepository.class);
    private final AiPrivacySettingsService service = new AiPrivacySettingsService(
            settingsRepository,
            currentUserService,
            conversationRepository,
            messageRepository,
            auditRepository,
            confirmationRepository
    );

    @Test
    void shouldCreateDefaultSettingsWithoutAiConsent() {
        when(settingsRepository.findByOwnerEmailIgnoreCase("ana@example.com")).thenReturn(Optional.empty());
        when(currentUserService.findUserByEmail("ana@example.com")).thenReturn(owner());
        when(settingsRepository.save(any(AiPrivacySettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiPrivacySettingsResponse response = service.get("ana@example.com");

        assertThat(response.aiEnabled()).isFalse();
        assertThat(response.consentGranted()).isFalse();
        assertThat(response.shareInvestmentProducts()).isFalse();
        assertThat(response.allowWriteOperations()).isFalse();
        assertThat(response.maskSensitiveValues()).isTrue();
        assertThat(response.retentionDays()).isEqualTo(30);
    }

    @Test
    void shouldRecordConsentAndEnableOnlyRequestedContext() {
        AiPrivacySettings settings = existingSettings();
        when(settingsRepository.findByOwnerEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(settings));
        when(settingsRepository.save(any(AiPrivacySettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiPrivacySettingsResponse response = service.update("ana@example.com", new AiPrivacySettingsRequest(
                true,
                true,
                true,
                false,
                true,
                false,
                true,
                true,
                true,
                45
        ));

        assertThat(response.aiEnabled()).isTrue();
        assertThat(response.consentGranted()).isTrue();
        assertThat(response.shareFinancialProfile()).isTrue();
        assertThat(response.shareMonthlySummary()).isFalse();
        assertThat(response.shareRecentTransactions()).isTrue();
        assertThat(response.shareSavingsGoals()).isFalse();
        assertThat(response.shareInvestmentProducts()).isTrue();
        assertThat(response.allowWriteOperations()).isTrue();
        assertThat(response.consentGrantedAt()).isNotNull();
        assertThat(response.retentionDays()).isEqualTo(45);
    }

    @Test
    void shouldBlockChatAndWriteWhenConsentIsMissing() {
        AiPrivacySettings settings = existingSettings();
        when(settingsRepository.findByOwnerEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(settings));

        assertThatThrownBy(() -> service.requireChatAllowed("ana@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Privacidade IA");

        settings.setAiEnabled(true);
        settings.setConsentGranted(true);

        assertThatThrownBy(() -> service.requireWriteAllowed("ana@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativadas");
    }

    @Test
    void shouldRequireExplicitInvestmentSharing() {
        AiPrivacySettings settings = existingSettings();
        settings.setAiEnabled(true);
        settings.setConsentGranted(true);
        settings.setShareInvestmentProducts(false);
        when(settingsRepository.findByOwnerEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(settings));

        assertThatThrownBy(() -> service.requireInvestmentProductsShared("ana@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Investimentos");

        settings.setShareInvestmentProducts(true);

        assertThat(service.requireInvestmentProductsShared("ana@example.com")).isSameAs(settings);
    }

    private AiPrivacySettings existingSettings() {
        AiPrivacySettings settings = new AiPrivacySettings();
        settings.setOwner(owner());
        settings.setAiEnabled(false);
        settings.setConsentGranted(false);
        settings.setShareFinancialProfile(false);
        settings.setShareMonthlySummary(false);
        settings.setShareRecentTransactions(false);
        settings.setShareSavingsGoals(false);
        settings.setShareInvestmentProducts(false);
        settings.setAllowWriteOperations(false);
        settings.setMaskSensitiveValues(true);
        settings.setRetentionDays(30);
        return settings;
    }

    private User owner() {
        User user = new User();
        user.setId(1L);
        user.setName("Ana");
        user.setEmail("ana@example.com");
        return user;
    }
}
