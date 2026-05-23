INSERT INTO tb_permission (name, description)
VALUES ('CATEGORY_SYSTEM_MANAGE', 'Criar e atualizar categorias padrão do sistema')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT IGNORE INTO tb_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r
JOIN tb_permission p ON p.name = 'CATEGORY_SYSTEM_MANAGE'
WHERE r.name = 'ROLE_ADMIN';
