CREATE TABLE tb_financial_workspace (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(140) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'PERSONAL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_financial_workspace_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_financial_workspace_owner ON tb_financial_workspace (owner_id);
CREATE INDEX idx_financial_workspace_type ON tb_financial_workspace (type);

CREATE TABLE tb_workspace_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    invited_by_id BIGINT NULL,
    joined_at TIMESTAMP NULL,
    removed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_workspace_member_user UNIQUE (workspace_id, user_id),
    CONSTRAINT fk_workspace_member_workspace FOREIGN KEY (workspace_id) REFERENCES tb_financial_workspace (id),
    CONSTRAINT fk_workspace_member_user FOREIGN KEY (user_id) REFERENCES tb_user (id),
    CONSTRAINT fk_workspace_member_invited_by FOREIGN KEY (invited_by_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_workspace_member_user_status ON tb_workspace_member (user_id, status);
CREATE INDEX idx_workspace_member_workspace ON tb_workspace_member (workspace_id);

CREATE TABLE tb_workspace_invitation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    invited_by_id BIGINT NOT NULL,
    invitee_email VARCHAR(180) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    token VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NULL,
    accepted_at TIMESTAMP NULL,
    canceled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_workspace_invitation_token UNIQUE (token),
    CONSTRAINT fk_workspace_invitation_workspace FOREIGN KEY (workspace_id) REFERENCES tb_financial_workspace (id),
    CONSTRAINT fk_workspace_invitation_invited_by FOREIGN KEY (invited_by_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_workspace_invitation_email_status ON tb_workspace_invitation (invitee_email, status);
CREATE INDEX idx_workspace_invitation_workspace ON tb_workspace_invitation (workspace_id);

INSERT INTO tb_financial_workspace (owner_id, name, type, active, created_at)
SELECT u.id,
       CONCAT('Espaco pessoal de ', SUBSTRING_INDEX(u.name, ' ', 1)),
       'PERSONAL',
       TRUE,
       CURRENT_TIMESTAMP
FROM tb_user u
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_financial_workspace w
    WHERE w.owner_id = u.id
      AND w.type = 'PERSONAL'
      AND w.active = TRUE
);

INSERT INTO tb_workspace_member (workspace_id, user_id, role, status, joined_at, created_at)
SELECT w.id,
       w.owner_id,
       'OWNER',
       'ACTIVE',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM tb_financial_workspace w
WHERE w.type = 'PERSONAL'
  AND NOT EXISTS (
      SELECT 1
      FROM tb_workspace_member m
      WHERE m.workspace_id = w.id
        AND m.user_id = w.owner_id
  );
