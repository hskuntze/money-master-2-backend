ALTER TABLE tb_theme
    ADD COLUMN favicon_url VARCHAR(500) NULL AFTER logo_url;

CREATE TABLE tb_access_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    occurred_at TIMESTAMP NOT NULL,
    method VARCHAR(12) NOT NULL,
    path VARCHAR(500) NOT NULL,
    query_string VARCHAR(1000) NULL,
    status_code INT NOT NULL,
    duration_ms BIGINT NOT NULL,
    principal VARCHAR(180) NULL,
    client_ip VARCHAR(80) NULL,
    user_agent VARCHAR(500) NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    INDEX idx_access_log_occurred_at (occurred_at),
    INDEX idx_access_log_status_code (status_code),
    INDEX idx_access_log_principal (principal),
    INDEX idx_access_log_path (path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_failure_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    occurred_at TIMESTAMP NOT NULL,
    method VARCHAR(12) NOT NULL,
    path VARCHAR(500) NOT NULL,
    query_string VARCHAR(1000) NULL,
    status_code INT NOT NULL,
    principal VARCHAR(180) NULL,
    client_ip VARCHAR(80) NULL,
    user_agent VARCHAR(500) NULL,
    exception_class VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    stack_trace LONGTEXT NULL,
    PRIMARY KEY (id),
    INDEX idx_failure_log_occurred_at (occurred_at),
    INDEX idx_failure_log_status_code (status_code),
    INDEX idx_failure_log_principal (principal),
    INDEX idx_failure_log_path (path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_email_settings (
    id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    host VARCHAR(180) NULL,
    port INT NULL,
    username VARCHAR(180) NULL,
    password VARCHAR(500) NULL,
    from_address VARCHAR(180) NULL,
    smtp_auth BOOLEAN NOT NULL DEFAULT TRUE,
    start_tls_enable BOOLEAN NOT NULL DEFAULT TRUE,
    start_tls_required BOOLEAN NOT NULL DEFAULT TRUE,
    ssl_enable BOOLEAN NOT NULL DEFAULT FALSE,
    debug BOOLEAN NOT NULL DEFAULT FALSE,
    connection_timeout_ms INT NULL,
    timeout_ms INT NULL,
    write_timeout_ms INT NULL,
    confirmation_base_url VARCHAR(500) NULL,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tb_permission (name, description) VALUES
('LOG_READ', 'Visualizar logs de acesso e falhas do sistema'),
('EMAIL_SETTINGS_MANAGE', 'Visualizar, testar e atualizar configurações de e-mail')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT IGNORE INTO tb_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r
JOIN tb_permission p ON p.name IN ('LOG_READ', 'EMAIL_SETTINGS_MANAGE')
WHERE r.name = 'ROLE_ADMIN';
