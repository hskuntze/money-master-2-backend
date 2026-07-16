CREATE TABLE tb_ai_privacy_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    ai_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    consent_granted BOOLEAN NOT NULL DEFAULT FALSE,
    share_financial_profile BOOLEAN NOT NULL DEFAULT FALSE,
    share_monthly_summary BOOLEAN NOT NULL DEFAULT FALSE,
    share_recent_transactions BOOLEAN NOT NULL DEFAULT FALSE,
    share_savings_goals BOOLEAN NOT NULL DEFAULT FALSE,
    allow_write_operations BOOLEAN NOT NULL DEFAULT FALSE,
    mask_sensitive_values BOOLEAN NOT NULL DEFAULT TRUE,
    retention_days INT NOT NULL DEFAULT 30,
    consent_granted_at TIMESTAMP NULL,
    consent_revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_privacy_settings_owner UNIQUE (owner_id),
    CONSTRAINT fk_ai_privacy_settings_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT ck_ai_privacy_retention_days CHECK (retention_days BETWEEN 1 AND 365)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ai_privacy_settings_owner ON tb_ai_privacy_settings (owner_id);

INSERT INTO tb_ai_privacy_settings (
    owner_id,
    ai_enabled,
    consent_granted,
    share_financial_profile,
    share_monthly_summary,
    share_recent_transactions,
    share_savings_goals,
    allow_write_operations,
    mask_sensitive_values,
    retention_days,
    created_at,
    updated_at
)
SELECT
    u.id,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    TRUE,
    30,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tb_user u
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_ai_privacy_settings s
    WHERE s.owner_id = u.id
);
