CREATE TABLE tb_security_audit_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    occurred_at TIMESTAMP NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    principal VARCHAR(180) NULL,
    client_ip VARCHAR(80) NULL,
    user_agent VARCHAR(500) NULL,
    method VARCHAR(12) NULL,
    path VARCHAR(500) NULL,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    details VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    INDEX idx_security_event_occurred_at (occurred_at),
    INDEX idx_security_event_type (event_type),
    INDEX idx_security_event_principal (principal),
    INDEX idx_security_event_client_ip (client_ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
