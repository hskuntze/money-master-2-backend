ALTER TABLE tb_user
    ADD COLUMN avatar_file_name VARCHAR(255) NULL AFTER last_login_at,
    ADD COLUMN avatar_content_type VARCHAR(80) NULL AFTER avatar_file_name,
    ADD COLUMN avatar_updated_at DATETIME(6) NULL AFTER avatar_content_type;
