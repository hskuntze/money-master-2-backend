-- Categorias financeiras iniciais baseadas no modelo atual da planilha de controle financeiro.
-- Todas são categorias padrão do sistema, disponíveis para todos os usuários.

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Salário', 'INCOME', 'wallet', '#16a34a', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Salário' AND type = 'INCOME');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Reembolso', 'INCOME', 'rotate-ccw', '#22c55e', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Reembolso' AND type = 'INCOME');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Rendimentos', 'INCOME', 'trending-up', '#15803d', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Rendimentos' AND type = 'INCOME');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Outras Receitas', 'INCOME', 'plus-circle', '#84cc16', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Outras Receitas' AND type = 'INCOME');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Alimentação', 'EXPENSE', 'utensils', '#ef4444', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Alimentação' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Mercado e Compras', 'EXPENSE', 'shopping-cart', '#f97316', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Mercado e Compras' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Moradia', 'EXPENSE', 'home', '#7c3aed', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Moradia' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Contas da Casa', 'EXPENSE', 'receipt', '#a855f7', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Contas da Casa' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Educação', 'EXPENSE', 'graduation-cap', '#2563eb', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Educação' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Transporte', 'EXPENSE', 'car', '#0ea5e9', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Transporte' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Assinaturas', 'EXPENSE', 'repeat', '#db2777', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Assinaturas' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Saúde', 'EXPENSE', 'heart-pulse', '#dc2626', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Saúde' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Pet', 'EXPENSE', 'paw-print', '#ca8a04', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Pet' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Lazer', 'EXPENSE', 'gamepad-2', '#9333ea', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Lazer' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Cuidados Pessoais', 'EXPENSE', 'scissors', '#ec4899', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Cuidados Pessoais' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Dívidas e Empréstimos', 'EXPENSE', 'landmark', '#b91c1c', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Dívidas e Empréstimos' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Investimentos e Reservas', 'EXPENSE', 'piggy-bank', '#059669', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Investimentos e Reservas' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Compras Online', 'EXPENSE', 'package', '#f59e0b', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Compras Online' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Impostos e Taxas', 'EXPENSE', 'file-text', '#64748b', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Impostos e Taxas' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Outros', 'EXPENSE', 'circle-ellipsis', '#6b7280', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Outros' AND type = 'EXPENSE');

INSERT INTO tb_category (owner_id, name, type, icon, color, system_default, active, created_at)
SELECT NULL, 'Transferências', 'TRANSFER', 'arrow-left-right', '#2563eb', TRUE, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM tb_category WHERE owner_id IS NULL AND name = 'Transferências' AND type = 'TRANSFER');
