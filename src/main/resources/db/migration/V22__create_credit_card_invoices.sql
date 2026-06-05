CREATE TABLE tb_credit_card (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    account_id BIGINT NULL,
    name VARCHAR(120) NOT NULL,
    brand VARCHAR(80) NULL,
    limit_amount DECIMAL(15,2) NULL,
    closing_day INT NOT NULL,
    due_day INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_credit_card_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_credit_card_account FOREIGN KEY (account_id) REFERENCES tb_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_credit_card_owner ON tb_credit_card (owner_id);
CREATE INDEX idx_credit_card_account ON tb_credit_card (account_id);

CREATE TABLE tb_credit_card_invoice (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    credit_card_id BIGINT NOT NULL,
    financial_period_id BIGINT NOT NULL,
    monthly_payable_plan_item_id BIGINT NULL,
    reference_month INT NOT NULL,
    reference_year INT NOT NULL,
    opening_date DATE NULL,
    closing_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    expected_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    final_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_card_invoice_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_card_invoice_card FOREIGN KEY (credit_card_id) REFERENCES tb_credit_card (id),
    CONSTRAINT fk_card_invoice_cycle FOREIGN KEY (financial_period_id) REFERENCES tb_financial_period (id),
    CONSTRAINT fk_card_invoice_payable FOREIGN KEY (monthly_payable_plan_item_id) REFERENCES tb_monthly_plan_item (id),
    CONSTRAINT uk_card_invoice_reference UNIQUE (credit_card_id, reference_month, reference_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_card_invoice_owner ON tb_credit_card_invoice (owner_id);
CREATE INDEX idx_card_invoice_card ON tb_credit_card_invoice (credit_card_id);
CREATE INDEX idx_card_invoice_cycle ON tb_credit_card_invoice (financial_period_id);
CREATE INDEX idx_card_invoice_payable ON tb_credit_card_invoice (monthly_payable_plan_item_id);
CREATE INDEX idx_card_invoice_status ON tb_credit_card_invoice (status);

CREATE TABLE tb_credit_card_invoice_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    purchase_date DATE NOT NULL,
    competence_date DATE NOT NULL,
    source_type VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    source_id BIGINT NULL,
    installment_number INT NULL,
    transaction_id BIGINT NULL,
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_item_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_invoice_item_invoice FOREIGN KEY (invoice_id) REFERENCES tb_credit_card_invoice (id),
    CONSTRAINT fk_invoice_item_category FOREIGN KEY (category_id) REFERENCES tb_category (id),
    CONSTRAINT fk_invoice_item_transaction FOREIGN KEY (transaction_id) REFERENCES tb_financial_transaction (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_invoice_item_owner ON tb_credit_card_invoice_item (owner_id);
CREATE INDEX idx_invoice_item_invoice ON tb_credit_card_invoice_item (invoice_id);
CREATE INDEX idx_invoice_item_category ON tb_credit_card_invoice_item (category_id);
CREATE INDEX idx_invoice_item_source ON tb_credit_card_invoice_item (source_type, source_id);
