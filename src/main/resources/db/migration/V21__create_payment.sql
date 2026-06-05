CREATE TABLE tb_payment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    financial_period_id BIGINT NOT NULL,
    payable_plan_item_id BIGINT NULL,
    income_plan_item_id BIGINT NULL,
    transaction_id BIGINT NULL,
    account_id BIGINT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    method VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    source VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    reversed_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_payment_cycle FOREIGN KEY (financial_period_id) REFERENCES tb_financial_period (id),
    CONSTRAINT fk_payment_payable FOREIGN KEY (payable_plan_item_id) REFERENCES tb_monthly_plan_item (id),
    CONSTRAINT fk_payment_income_plan FOREIGN KEY (income_plan_item_id) REFERENCES tb_monthly_plan_item (id),
    CONSTRAINT fk_payment_transaction FOREIGN KEY (transaction_id) REFERENCES tb_financial_transaction (id),
    CONSTRAINT fk_payment_account FOREIGN KEY (account_id) REFERENCES tb_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payment_owner_date ON tb_payment (owner_id, payment_date);
CREATE INDEX idx_payment_cycle ON tb_payment (financial_period_id);
CREATE INDEX idx_payment_payable ON tb_payment (payable_plan_item_id);
CREATE INDEX idx_payment_income_plan ON tb_payment (income_plan_item_id);
CREATE INDEX idx_payment_transaction ON tb_payment (transaction_id);
CREATE INDEX idx_payment_status ON tb_payment (status);
