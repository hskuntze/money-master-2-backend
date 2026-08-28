CREATE TABLE tb_ai_command_audit_reversal (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    audit_id BIGINT NOT NULL,
    reference_type VARCHAR(80) NOT NULL,
    reference_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    result_json LONGTEXT NULL,
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_command_audit_reversal_audit UNIQUE (audit_id),
    CONSTRAINT fk_ai_command_audit_reversal_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_ai_command_audit_reversal_audit FOREIGN KEY (audit_id) REFERENCES tb_ai_command_audit (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ai_command_audit_reversal_owner ON tb_ai_command_audit_reversal (owner_id, created_at);
CREATE INDEX idx_ai_command_audit_reversal_audit ON tb_ai_command_audit_reversal (audit_id);
