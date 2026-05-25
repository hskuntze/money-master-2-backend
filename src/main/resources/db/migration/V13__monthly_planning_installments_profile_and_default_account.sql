-- Reposiciona o Money Master para planejamento mensal como eixo central.
-- Estratégia segura: Account continua existindo como compatibilidade interna, mas cada usuário passa a ter uma conta principal/default.

ALTER TABLE tb_account
    ADD COLUMN internal_default BOOLEAN NOT NULL DEFAULT FALSE AFTER active;

-- Cria uma conta principal para usuários que ainda não possuem conta alguma.
INSERT INTO tb_account (owner_id, name, type, initial_balance, active, internal_default, created_at)
SELECT u.id, 'Conta principal', 'CHECKING', 0.00, TRUE, TRUE, CURRENT_TIMESTAMP
FROM tb_user u
WHERE NOT EXISTS (
    SELECT 1 FROM tb_account a WHERE a.owner_id = u.id
);

-- Define uma única conta principal por usuário: a menor conta existente fica ativa/default; as demais ficam inativas para compatibilidade histórica.
UPDATE tb_account a
JOIN (
    SELECT owner_id, MIN(id) AS default_account_id
    FROM tb_account
    GROUP BY owner_id
) selected_account ON selected_account.owner_id = a.owner_id
SET a.internal_default = CASE WHEN a.id = selected_account.default_account_id THEN TRUE ELSE FALSE END,
    a.active = CASE WHEN a.id = selected_account.default_account_id THEN TRUE ELSE FALSE END,
    a.name = CASE WHEN a.id = selected_account.default_account_id THEN 'Conta principal' ELSE a.name END,
    a.updated_at = CURRENT_TIMESTAMP;

-- Reassocia dados financeiros antigos para a conta principal interna do usuário.
UPDATE tb_financial_transaction t
JOIN tb_account default_account
  ON default_account.owner_id = t.owner_id
 AND default_account.internal_default = TRUE
SET t.account_id = default_account.id,
    t.updated_at = CURRENT_TIMESTAMP
WHERE t.account_id <> default_account.id;

UPDATE tb_monthly_plan_item i
JOIN tb_account default_account
  ON default_account.owner_id = i.owner_id
 AND default_account.internal_default = TRUE
SET i.account_id = default_account.id,
    i.updated_at = CURRENT_TIMESTAMP
WHERE i.account_id IS NULL OR i.account_id <> default_account.id;

UPDATE tb_savings_jar j
JOIN tb_account default_account
  ON default_account.owner_id = j.owner_id
 AND default_account.internal_default = TRUE
SET j.linked_account_id = default_account.id,
    j.updated_at = CURRENT_TIMESTAMP
WHERE j.linked_account_id IS NOT NULL
  AND j.linked_account_id <> default_account.id;

CREATE INDEX idx_account_owner_internal_default ON tb_account (owner_id, internal_default);

CREATE TABLE tb_installment_purchase (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    account_id BIGINT NULL,
    category_id BIGINT NULL,
    description VARCHAR(255) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    installment_count INT NOT NULL,
    installment_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    purchase_date DATE NOT NULL,
    first_due_date DATE NOT NULL,
    last_due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_installment_purchase_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_installment_purchase_account FOREIGN KEY (account_id) REFERENCES tb_account (id),
    CONSTRAINT fk_installment_purchase_category FOREIGN KEY (category_id) REFERENCES tb_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_installment_purchase_owner_status ON tb_installment_purchase (owner_id, status);
CREATE INDEX idx_installment_purchase_owner_dates ON tb_installment_purchase (owner_id, first_due_date, last_due_date);

CREATE TABLE tb_installment_purchase_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    purchase_id BIGINT NOT NULL,
    financial_period_id BIGINT NOT NULL,
    monthly_plan_item_id BIGINT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_installment_entry_purchase_number UNIQUE (purchase_id, installment_number),
    CONSTRAINT fk_installment_entry_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_installment_entry_purchase FOREIGN KEY (purchase_id) REFERENCES tb_installment_purchase (id),
    CONSTRAINT fk_installment_entry_period FOREIGN KEY (financial_period_id) REFERENCES tb_financial_period (id),
    CONSTRAINT fk_installment_entry_plan_item FOREIGN KEY (monthly_plan_item_id) REFERENCES tb_monthly_plan_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_installment_entry_owner_due ON tb_installment_purchase_entry (owner_id, due_date);
CREATE INDEX idx_installment_entry_period ON tb_installment_purchase_entry (financial_period_id);
CREATE INDEX idx_installment_entry_plan_item ON tb_installment_purchase_entry (monthly_plan_item_id);

CREATE TABLE tb_user_financial_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    age INT NULL,
    age_range VARCHAR(80) NULL,
    profession VARCHAR(160) NULL,
    approximate_monthly_income DECIMAL(15,2) NULL,
    current_financial_situation VARCHAR(1000) NULL,
    spending_habits VARCHAR(1000) NULL,
    financial_objectives VARCHAR(1000) NULL,
    short_term_goals VARCHAR(1000) NULL,
    medium_term_goals VARCHAR(1000) NULL,
    long_term_goals VARCHAR(1000) NULL,
    risk_tolerance VARCHAR(80) NULL,
    investment_knowledge VARCHAR(80) NULL,
    investor_profile VARCHAR(80) NULL,
    financial_preferences VARCHAR(1000) NULL,
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    onboarding_completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_financial_profile_owner UNIQUE (owner_id),
    CONSTRAINT fk_user_financial_profile_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_financial_profile_owner ON tb_user_financial_profile (owner_id);

CREATE TABLE tb_financial_reference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NULL,
    title VARCHAR(180) NOT NULL,
    type VARCHAR(30) NOT NULL,
    url VARCHAR(1000) NULL,
    description VARCHAR(2000) NULL,
    source VARCHAR(180) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_financial_reference_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_financial_reference_owner_active ON tb_financial_reference (owner_id, active);
CREATE INDEX idx_financial_reference_type ON tb_financial_reference (type);

INSERT INTO tb_user_financial_profile (owner_id, onboarding_completed, created_at)
SELECT u.id, FALSE, CURRENT_TIMESTAMP
FROM tb_user u
WHERE NOT EXISTS (
    SELECT 1 FROM tb_user_financial_profile p WHERE p.owner_id = u.id
);
