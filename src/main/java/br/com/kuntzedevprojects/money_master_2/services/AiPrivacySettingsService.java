package br.com.kuntzedevprojects.money_master_2.services;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class AiPrivacySettingsService {

    private static final int DEFAULT_RETENTION_DAYS = 30;
    private static final int MIN_RETENTION_DAYS = 1;
    private static final int MAX_RETENTION_DAYS = 365;

    private final AiPrivacySettingsRepository settingsRepository;
    private final CurrentUserService currentUserService;
    private final AiChatConversationRepository conversationRepository;
    private final AiChatMessageRepository messageRepository;
    private final AiCommandAuditRepository auditRepository;
    private final AiCommandConfirmationRepository confirmationRepository;

    public AiPrivacySettingsService(
            AiPrivacySettingsRepository settingsRepository,
            CurrentUserService currentUserService,
            AiChatConversationRepository conversationRepository,
            AiChatMessageRepository messageRepository,
            AiCommandAuditRepository auditRepository,
            AiCommandConfirmationRepository confirmationRepository
    ) {
        this.settingsRepository = settingsRepository;
        this.currentUserService = currentUserService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.auditRepository = auditRepository;
        this.confirmationRepository = confirmationRepository;
    }

    @Transactional
    public AiPrivacySettingsResponse get(String ownerEmail) {
        return AiPrivacySettingsResponse.from(resolve(ownerEmail));
    }

    @Transactional
    public AiPrivacySettingsResponse update(String ownerEmail, AiPrivacySettingsRequest request) {
        AiPrivacySettings settings = resolve(ownerEmail);
        boolean wasAllowed = isChatAllowed(settings);

        boolean aiEnabled = valueOrDefault(request.aiEnabled(), settings.isAiEnabled());
        boolean consentGranted = valueOrDefault(request.consentGranted(), settings.isConsentGranted());
        boolean allowed = aiEnabled && consentGranted;

        settings.setAiEnabled(aiEnabled);
        settings.setConsentGranted(consentGranted);
        settings.setShareFinancialProfile(allowed && valueOrDefault(request.shareFinancialProfile(), settings.isShareFinancialProfile()));
        settings.setShareMonthlySummary(allowed && valueOrDefault(request.shareMonthlySummary(), settings.isShareMonthlySummary()));
        settings.setShareRecentTransactions(allowed && valueOrDefault(request.shareRecentTransactions(), settings.isShareRecentTransactions()));
        settings.setShareSavingsGoals(allowed && valueOrDefault(request.shareSavingsGoals(), settings.isShareSavingsGoals()));
        settings.setShareInvestmentProducts(allowed && valueOrDefault(request.shareInvestmentProducts(), settings.isShareInvestmentProducts()));
        settings.setAllowWriteOperations(allowed && valueOrDefault(request.allowWriteOperations(), settings.isAllowWriteOperations()));
        settings.setMaskSensitiveValues(valueOrDefault(request.maskSensitiveValues(), settings.isMaskSensitiveValues()));
        settings.setRetentionDays(normalizeRetentionDays(request.retentionDays(), settings.getRetentionDays()));

        Instant now = Instant.now();
        if (!wasAllowed && allowed) {
            settings.setConsentGrantedAt(now);
            settings.setConsentRevokedAt(null);
        }
        if (wasAllowed && !allowed) {
            settings.setConsentRevokedAt(now);
        }

        return AiPrivacySettingsResponse.from(settingsRepository.save(settings));
    }

    @Transactional
    public void deleteChatHistory(String ownerEmail) {
        List<Long> conversationIds = conversationRepository.findIdsByOwnerEmail(ownerEmail);
        if (conversationIds.isEmpty()) {
            return;
        }
        auditRepository.detachConversations(conversationIds);
        confirmationRepository.detachConversations(conversationIds);
        messageRepository.deleteByConversationIds(conversationIds);
        conversationRepository.deleteAllByIdInBatch(conversationIds);
    }

    @Transactional
    public void purgeExpiredChatMessages(String ownerEmail, AiPrivacySettings settings) {
        int retentionDays = normalizeRetentionDays(settings.getRetentionDays(), DEFAULT_RETENTION_DAYS);
        List<Long> conversationIds = conversationRepository.findIdsByOwnerEmail(ownerEmail);
        if (conversationIds.isEmpty()) {
            return;
        }
        Instant threshold = Instant.now().minusSeconds(retentionDays * 24L * 60L * 60L);
        messageRepository.deleteByConversationIdsAndCreatedAtBefore(conversationIds, threshold);
    }

    @Transactional
    public AiPrivacySettings requireChatAllowed(String ownerEmail) {
        AiPrivacySettings settings = resolve(ownerEmail);
        if (!isChatAllowed(settings)) {
            throw new BusinessException("Ative a IA e conceda consentimento em Privacidade IA antes de enviar dados financeiros ao assistente.");
        }
        return settings;
    }

    @Transactional
    public void requireWriteAllowed(String ownerEmail) {
        AiPrivacySettings settings = requireChatAllowed(ownerEmail);
        if (!settings.isAllowWriteOperations()) {
            throw new BusinessException("As operacoes de escrita pela IA estao desativadas nas configuracoes de Privacidade IA.");
        }
    }

    @Transactional
    public AiPrivacySettings requireInvestmentProductsShared(String ownerEmail) {
        AiPrivacySettings settings = requireChatAllowed(ownerEmail);
        if (!settings.isShareInvestmentProducts()) {
            throw new BusinessException("Investimentos e produtos financeiros nao estao compartilhados com a IA nas configuracoes de Privacidade IA.");
        }
        return settings;
    }

    @Transactional
    public AiPrivacySettings resolve(String ownerEmail) {
        return settingsRepository.findByOwnerEmailIgnoreCase(ownerEmail)
                .orElseGet(() -> createDefault(ownerEmail));
    }

    public boolean isChatAllowed(AiPrivacySettings settings) {
        return settings != null && settings.isAiEnabled() && settings.isConsentGranted();
    }

    private AiPrivacySettings createDefault(String ownerEmail) {
        User owner = currentUserService.findUserByEmail(ownerEmail);
        AiPrivacySettings settings = new AiPrivacySettings();
        settings.setOwner(owner);
        settings.setAiEnabled(false);
        settings.setConsentGranted(false);
        settings.setShareFinancialProfile(false);
        settings.setShareMonthlySummary(false);
        settings.setShareRecentTransactions(false);
        settings.setShareSavingsGoals(false);
        settings.setShareInvestmentProducts(false);
        settings.setAllowWriteOperations(false);
        settings.setMaskSensitiveValues(true);
        settings.setRetentionDays(DEFAULT_RETENTION_DAYS);
        return settingsRepository.save(settings);
    }

    private boolean valueOrDefault(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private int normalizeRetentionDays(Integer requested, Integer current) {
        int value = requested == null ? (current == null ? DEFAULT_RETENTION_DAYS : current) : requested;
        if (value < MIN_RETENTION_DAYS || value > MAX_RETENTION_DAYS) {
            throw new BusinessException("A retencao do historico da IA deve ficar entre 1 e 365 dias.");
        }
        return value;
    }
}
