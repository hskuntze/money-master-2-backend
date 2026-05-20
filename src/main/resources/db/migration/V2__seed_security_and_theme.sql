INSERT INTO tb_permission (name, description) VALUES
('USER_READ', 'Listar e visualizar usuários'),
('USER_MANAGE', 'Criar e atualizar usuários'),
('ROLE_READ', 'Listar perfis e permissões'),
('THEME_MANAGE', 'Atualizar tema ativo'),
('FINANCE_READ', 'Visualizar dados financeiros'),
('FINANCE_MANAGE', 'Criar e atualizar dados financeiros'),
('AI_CHAT_USE', 'Usar chat financeiro com IA')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO tb_role (name, description) VALUES
('ROLE_ADMIN', 'Administrador geral do Money Master'),
('ROLE_USER', 'Usuário padrão do sistema')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT IGNORE INTO tb_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r
JOIN tb_permission p ON p.name IN ('USER_READ', 'USER_MANAGE', 'ROLE_READ', 'THEME_MANAGE', 'FINANCE_READ', 'FINANCE_MANAGE', 'AI_CHAT_USE')
WHERE r.name = 'ROLE_ADMIN';

INSERT IGNORE INTO tb_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r
JOIN tb_permission p ON p.name IN ('FINANCE_READ', 'FINANCE_MANAGE', 'AI_CHAT_USE')
WHERE r.name = 'ROLE_USER';

INSERT INTO tb_theme (
    active, app_name, logo_url, primary_color, secondary_color, accent_color,
    background_color, text_color, card_color, login_title, login_subtitle
)
SELECT TRUE, 'Money Master', NULL, '#2563eb', '#0f172a', '#22c55e', '#f8fafc', '#0f172a', '#ffffff',
       'Controle financeiro inteligente', 'Organize gastos, receitas e decisões com apoio de IA.'
WHERE NOT EXISTS (SELECT 1 FROM tb_theme WHERE active = TRUE);
