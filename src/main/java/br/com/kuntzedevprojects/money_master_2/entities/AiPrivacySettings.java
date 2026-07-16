package br.com.kuntzedevprojects.money_master_2.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "tb_ai_privacy_settings",
        indexes = {
                @Index(name = "idx_ai_privacy_settings_owner", columnList = "owner_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_privacy_settings_owner", columnNames = "owner_id")
        }
)
public class AiPrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "ai_enabled", nullable = false)
    private boolean aiEnabled;

    @Column(name = "consent_granted", nullable = false)
    private boolean consentGranted;

    @Column(name = "share_financial_profile", nullable = false)
    private boolean shareFinancialProfile;

    @Column(name = "share_monthly_summary", nullable = false)
    private boolean shareMonthlySummary;

    @Column(name = "share_recent_transactions", nullable = false)
    private boolean shareRecentTransactions;

    @Column(name = "share_savings_goals", nullable = false)
    private boolean shareSavingsGoals;

    @Column(name = "allow_write_operations", nullable = false)
    private boolean allowWriteOperations;

    @Column(name = "mask_sensitive_values", nullable = false)
    private boolean maskSensitiveValues = true;

    @Column(name = "retention_days", nullable = false)
    private Integer retentionDays = 30;

    @Column(name = "consent_granted_at")
    private Instant consentGrantedAt;

    @Column(name = "consent_revoked_at")
    private Instant consentRevokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public boolean isConsentGranted() {
        return consentGranted;
    }

    public void setConsentGranted(boolean consentGranted) {
        this.consentGranted = consentGranted;
    }

    public boolean isShareFinancialProfile() {
        return shareFinancialProfile;
    }

    public void setShareFinancialProfile(boolean shareFinancialProfile) {
        this.shareFinancialProfile = shareFinancialProfile;
    }

    public boolean isShareMonthlySummary() {
        return shareMonthlySummary;
    }

    public void setShareMonthlySummary(boolean shareMonthlySummary) {
        this.shareMonthlySummary = shareMonthlySummary;
    }

    public boolean isShareRecentTransactions() {
        return shareRecentTransactions;
    }

    public void setShareRecentTransactions(boolean shareRecentTransactions) {
        this.shareRecentTransactions = shareRecentTransactions;
    }

    public boolean isShareSavingsGoals() {
        return shareSavingsGoals;
    }

    public void setShareSavingsGoals(boolean shareSavingsGoals) {
        this.shareSavingsGoals = shareSavingsGoals;
    }

    public boolean isAllowWriteOperations() {
        return allowWriteOperations;
    }

    public void setAllowWriteOperations(boolean allowWriteOperations) {
        this.allowWriteOperations = allowWriteOperations;
    }

    public boolean isMaskSensitiveValues() {
        return maskSensitiveValues;
    }

    public void setMaskSensitiveValues(boolean maskSensitiveValues) {
        this.maskSensitiveValues = maskSensitiveValues;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public Instant getConsentGrantedAt() {
        return consentGrantedAt;
    }

    public void setConsentGrantedAt(Instant consentGrantedAt) {
        this.consentGrantedAt = consentGrantedAt;
    }

    public Instant getConsentRevokedAt() {
        return consentRevokedAt;
    }

    public void setConsentRevokedAt(Instant consentRevokedAt) {
        this.consentRevokedAt = consentRevokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
