CREATE TABLE tb_installment_anticipation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    purchase_id BIGINT NOT NULL,
    credit_card_id BIGINT NULL,
    target_invoice_id BIGINT NULL,
    financial_period_id BIGINT NOT NULL,
    anticipation_date DATE NOT NULL,
    original_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    anticipated_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_installment_anticipation_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_installment_anticipation_purchase FOREIGN KEY (purchase_id) REFERENCES tb_installment_purchase (id),
    CONSTRAINT fk_installment_anticipation_credit_card FOREIGN KEY (credit_card_id) REFERENCES tb_credit_card (id),
    CONSTRAINT fk_installment_anticipation_invoice FOREIGN KEY (target_invoice_id) REFERENCES tb_credit_card_invoice (id),
    CONSTRAINT fk_installment_anticipation_cycle FOREIGN KEY (financial_period_id) REFERENCES tb_financial_period (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_installment_anticipation_owner ON tb_installment_anticipation (owner_id);
CREATE INDEX idx_installment_anticipation_purchase ON tb_installment_anticipation (purchase_id);
CREATE INDEX idx_installment_anticipation_invoice ON tb_installment_anticipation (target_invoice_id);

CREATE TABLE tb_installment_anticipation_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    anticipation_id BIGINT NOT NULL,
    installment_entry_id BIGINT NOT NULL,
    original_due_date DATE NOT NULL,
    original_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    anticipated_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_installment_anticipation_item_parent FOREIGN KEY (anticipation_id) REFERENCES tb_installment_anticipation (id),
    CONSTRAINT fk_installment_anticipation_item_entry FOREIGN KEY (installment_entry_id) REFERENCES tb_installment_purchase_entry (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_installment_anticipation_item_parent ON tb_installment_anticipation_item (anticipation_id);
CREATE INDEX idx_installment_anticipation_item_entry ON tb_installment_anticipation_item (installment_entry_id);
