CREATE TABLE tb_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES tb_role (id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES tb_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(140) NOT NULL,
    email VARCHAR(180) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES tb_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES tb_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    replaced_by_token_hash VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_token_user ON tb_refresh_token (user_id);

CREATE TABLE tb_email_confirmation_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_confirmation_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_confirmation_user FOREIGN KEY (user_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_email_confirmation_user ON tb_email_confirmation_token (user_id);

CREATE TABLE tb_theme (
    id BIGINT NOT NULL AUTO_INCREMENT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    app_name VARCHAR(120) NOT NULL,
    logo_url VARCHAR(500),
    primary_color VARCHAR(20) NOT NULL,
    secondary_color VARCHAR(20) NOT NULL,
    accent_color VARCHAR(20) NOT NULL,
    background_color VARCHAR(20) NOT NULL,
    text_color VARCHAR(20) NOT NULL,
    card_color VARCHAR(20) NOT NULL,
    login_title VARCHAR(140),
    login_subtitle VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(30) NOT NULL,
    initial_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_account_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_account_owner ON tb_account (owner_id);

CREATE TABLE tb_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(30) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(20),
    system_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_category_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_category_owner ON tb_category (owner_id);
CREATE INDEX idx_category_name_type ON tb_category (name, type);

CREATE TABLE tb_financial_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    type VARCHAR(30) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    occurred_on DATE NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ai_raw_message VARCHAR(2000),
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_transaction_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES tb_account (id),
    CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES tb_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_transaction_owner_date ON tb_financial_transaction (owner_id, occurred_on);
CREATE INDEX idx_transaction_account ON tb_financial_transaction (account_id);
CREATE INDEX idx_transaction_category ON tb_financial_transaction (category_id);
