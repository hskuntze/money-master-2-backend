-- Money Master 2 - Fundação para assinatura/billing
-- Esta migration é propositalmente desacoplada do código atual: cria estrutura para o módulo futuro
-- sem quebrar o ddl-auto=validate, pois apenas adiciona tabelas e permissões.

INSERT INTO tb_permission (name, description) VALUES
('SUBSCRIPTION_READ', 'Visualizar dados de assinatura e plano'),
('SUBSCRIPTION_MANAGE', 'Gerenciar planos, assinaturas e eventos de billing')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT IGNORE INTO tb_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r
JOIN tb_permission p ON p.name IN ('SUBSCRIPTION_READ', 'SUBSCRIPTION_MANAGE')
WHERE r.name = 'ROLE_ADMIN';

INSERT IGNORE INTO tb_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r
JOIN tb_permission p ON p.name IN ('SUBSCRIPTION_READ')
WHERE r.name = 'ROLE_USER';

CREATE TABLE IF NOT EXISTS tb_subscription_plan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    price_cents INT NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    billing_interval VARCHAR(30) NOT NULL DEFAULT 'MONTH',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    allow_ai_chat BOOLEAN NOT NULL DEFAULT FALSE,
    max_accounts INT NULL,
    max_transactions_month INT NULL,
    max_ai_messages_month INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_subscription_plan_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tb_billing_customer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_customer_id VARCHAR(180) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_billing_customer_user_provider UNIQUE (user_id, provider),
    CONSTRAINT uk_billing_customer_provider_id UNIQUE (provider, provider_customer_id),
    CONSTRAINT fk_billing_customer_user FOREIGN KEY (user_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tb_user_subscription (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    billing_customer_id BIGINT NULL,
    provider VARCHAR(40) NULL,
    provider_subscription_id VARCHAR(180) NULL,
    provider_status VARCHAR(80) NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'FREE',
    current_period_start TIMESTAMP NULL,
    current_period_end TIMESTAMP NULL,
    trial_ends_at TIMESTAMP NULL,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_subscription_user UNIQUE (user_id),
    CONSTRAINT uk_user_subscription_provider_id UNIQUE (provider, provider_subscription_id),
    CONSTRAINT fk_user_subscription_user FOREIGN KEY (user_id) REFERENCES tb_user (id),
    CONSTRAINT fk_user_subscription_plan FOREIGN KEY (plan_id) REFERENCES tb_subscription_plan (id),
    CONSTRAINT fk_user_subscription_billing_customer FOREIGN KEY (billing_customer_id) REFERENCES tb_billing_customer (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_subscription_status ON tb_user_subscription (status);
CREATE INDEX idx_user_subscription_period_end ON tb_user_subscription (current_period_end);

CREATE TABLE IF NOT EXISTS tb_billing_webhook_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(40) NOT NULL,
    event_id VARCHAR(180) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    processing_status VARCHAR(40) NOT NULL DEFAULT 'RECEIVED',
    payload_json LONGTEXT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    error_message VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_billing_webhook_event UNIQUE (provider, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_billing_webhook_event_status ON tb_billing_webhook_event (processing_status);
CREATE INDEX idx_billing_webhook_event_type ON tb_billing_webhook_event (event_type);

INSERT INTO tb_subscription_plan (
    code, name, description, price_cents, currency, billing_interval,
    active, allow_ai_chat, max_accounts, max_transactions_month, max_ai_messages_month
) VALUES
('FREE', 'Gratuito', 'Plano gratuito inicial para uso básico do Money Master.', 0, 'BRL', 'MONTH', TRUE, FALSE, 2, 100, 0),
('PLUS', 'Plus', 'Plano pago para uso completo com IA financeira.', 1990, 'BRL', 'MONTH', TRUE, TRUE, NULL, NULL, 300)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    price_cents = VALUES(price_cents),
    currency = VALUES(currency),
    billing_interval = VALUES(billing_interval),
    active = VALUES(active),
    allow_ai_chat = VALUES(allow_ai_chat),
    max_accounts = VALUES(max_accounts),
    max_transactions_month = VALUES(max_transactions_month),
    max_ai_messages_month = VALUES(max_ai_messages_month),
    updated_at = CURRENT_TIMESTAMP;
