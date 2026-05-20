CREATE TABLE tb_savings_jar (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    linked_account_id BIGINT NULL,
    name VARCHAR(120) NOT NULL,
    institution_name VARCHAR(120) NULL,
    description VARCHAR(255) NULL,
    target_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    target_date DATE NULL,
    image_url VARCHAR(500) NULL,
    icon VARCHAR(50) NULL,
    color VARCHAR(20) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    yield_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    yield_calculation_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    yield_percentage DECIMAL(7,4) NOT NULL DEFAULT 0.0000,
    business_days_only BOOLEAN NOT NULL DEFAULT TRUE,
    use_brazilian_holidays BOOLEAN NOT NULL DEFAULT FALSE,
    yield_start_date DATE NULL,
    last_yield_calculation_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_savings_jar_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_savings_jar_linked_account FOREIGN KEY (linked_account_id) REFERENCES tb_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_savings_jar_owner ON tb_savings_jar (owner_id);
CREATE INDEX idx_savings_jar_owner_bank_name ON tb_savings_jar (owner_id, institution_name, name);
CREATE INDEX idx_savings_jar_yield_enabled ON tb_savings_jar (yield_enabled, active, last_yield_calculation_date);

CREATE TABLE tb_savings_jar_movement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    savings_jar_id BIGINT NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    occurred_on DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    base_amount DECIMAL(15,2) NULL,
    rate_applied DECIMAL(12,6) NULL,
    rate_reference VARCHAR(80) NULL,
    ai_raw_message VARCHAR(2000) NULL,
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_savings_jar_movement_jar FOREIGN KEY (savings_jar_id) REFERENCES tb_savings_jar (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_savings_jar_movement_jar_date ON tb_savings_jar_movement (savings_jar_id, occurred_on);
CREATE INDEX idx_savings_jar_movement_type ON tb_savings_jar_movement (movement_type);

CREATE TABLE tb_bcb_daily_rate (
    id BIGINT NOT NULL AUTO_INCREMENT,
    series_code INT NOT NULL,
    reference_date DATE NOT NULL,
    value DECIMAL(12,6) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bcb_daily_rate_series_date UNIQUE (series_code, reference_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bcb_daily_rate_series_date ON tb_bcb_daily_rate (series_code, reference_date);
