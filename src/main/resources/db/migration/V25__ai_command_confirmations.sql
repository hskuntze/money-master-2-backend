CREATE TABLE tb_ai_command_confirmation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    conversation_id BIGINT NULL,
    token_hash VARCHAR(128) NOT NULL,
    command_hash VARCHAR(128) NOT NULL,
    command_json LONGTEXT NOT NULL,
    reason VARCHAR(1000) NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_command_confirmation_hash UNIQUE (token_hash),
    CONSTRAINT fk_ai_command_confirmation_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_ai_command_confirmation_conversation FOREIGN KEY (conversation_id) REFERENCES tb_ai_chat_conversation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ai_command_confirmation_owner ON tb_ai_command_confirmation (owner_id, created_at);
CREATE INDEX idx_ai_command_confirmation_expires ON tb_ai_command_confirmation (expires_at);
