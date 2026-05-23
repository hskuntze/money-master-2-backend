CREATE TABLE tb_ai_chat_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    conversation_key VARCHAR(80) NOT NULL,
    title VARCHAR(180),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_chat_conversation_owner_key UNIQUE (owner_id, conversation_key),
    CONSTRAINT fk_ai_chat_conversation_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id)
);

CREATE INDEX idx_ai_chat_conversation_owner ON tb_ai_chat_conversation (owner_id);

CREATE TABLE tb_ai_chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES tb_ai_chat_conversation (id)
);

CREATE INDEX idx_ai_chat_message_conversation ON tb_ai_chat_message (conversation_id, created_at);

CREATE TABLE tb_ai_command_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    conversation_id BIGINT NULL,
    command_type VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    dry_run BOOLEAN NOT NULL DEFAULT TRUE,
    command_json LONGTEXT NOT NULL,
    result_json LONGTEXT NULL,
    error_message VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_command_audit_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_ai_command_audit_conversation FOREIGN KEY (conversation_id) REFERENCES tb_ai_chat_conversation (id)
);

CREATE INDEX idx_ai_command_audit_owner ON tb_ai_command_audit (owner_id, created_at);
CREATE INDEX idx_ai_command_audit_conversation ON tb_ai_command_audit (conversation_id, created_at);
