CREATE TABLE tb_debt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    account_id BIGINT NULL,
    category_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    amortization_method VARCHAR(40) NOT NULL DEFAULT 'CONSTANT_PRINCIPAL',
    principal_amount DECIMAL(15,2) NOT NULL,
    installment_amount DECIMAL(15,2) NULL,
    annual_interest_rate DECIMAL(8,4) NULL,
    annual_cet_rate DECIMAL(8,4) NULL,
    monthly_fee_amount DECIMAL(15,2) NULL,
    installment_count INT NOT NULL,
    start_date DATE NOT NULL,
    first_due_date DATE NOT NULL,
    last_due_date DATE NOT NULL,
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    canceled_at TIMESTAMP NULL,
    paid_off_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_debt_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_debt_account FOREIGN KEY (account_id) REFERENCES tb_account (id),
    CONSTRAINT fk_debt_category FOREIGN KEY (category_id) REFERENCES tb_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_debt_owner_status ON tb_debt (owner_id, status);
CREATE INDEX idx_debt_owner_dates ON tb_debt (owner_id, start_date, first_due_date);

CREATE TABLE tb_debt_installment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    debt_id BIGINT NOT NULL,
    financial_period_id BIGINT NOT NULL,
    monthly_plan_item_id BIGINT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    fee_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL,
    balance_after_payment DECIMAL(15,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_debt_installment_number UNIQUE (debt_id, installment_number),
    CONSTRAINT fk_debt_installment_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_debt_installment_debt FOREIGN KEY (debt_id) REFERENCES tb_debt (id),
    CONSTRAINT fk_debt_installment_cycle FOREIGN KEY (financial_period_id) REFERENCES tb_financial_period (id),
    CONSTRAINT fk_debt_installment_plan_item FOREIGN KEY (monthly_plan_item_id) REFERENCES tb_monthly_plan_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_debt_installment_owner_due ON tb_debt_installment (owner_id, due_date);
CREATE INDEX idx_debt_installment_cycle ON tb_debt_installment (financial_period_id);
CREATE INDEX idx_debt_installment_plan_item ON tb_debt_installment (monthly_plan_item_id);
CREATE INDEX idx_debt_installment_status ON tb_debt_installment (status);
