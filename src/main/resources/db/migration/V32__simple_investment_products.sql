CREATE TABLE tb_investment_product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    linked_account_id BIGINT NULL,
    name VARCHAR(120) NOT NULL,
    type_name VARCHAR(80) NOT NULL,
    institution_name VARCHAR(120) NULL,
    liquidity VARCHAR(120) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_investment_product_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_investment_product_account FOREIGN KEY (linked_account_id) REFERENCES tb_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_investment_product_owner ON tb_investment_product (owner_id);
CREATE INDEX idx_investment_product_account ON tb_investment_product (linked_account_id);
CREATE INDEX idx_investment_product_active ON tb_investment_product (owner_id, active);

CREATE TABLE tb_investment_movement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    investment_product_id BIGINT NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    occurred_on DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_investment_movement_product FOREIGN KEY (investment_product_id) REFERENCES tb_investment_product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_investment_movement_product_date ON tb_investment_movement (investment_product_id, occurred_on);
CREATE INDEX idx_investment_movement_type ON tb_investment_movement (movement_type);
