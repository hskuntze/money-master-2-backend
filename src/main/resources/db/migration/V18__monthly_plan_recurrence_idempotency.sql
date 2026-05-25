-- Idempotência e propagação segura de itens recorrentes do planejamento mensal.
-- Não apaga dados, não cria ciclos e não vincula faturas antigas automaticamente.

DELIMITER $$

CREATE PROCEDURE add_monthly_plan_item_column_if_missing_v18(
    IN column_name_param VARCHAR(64),
    IN column_ddl_param TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tb_monthly_plan_item'
          AND COLUMN_NAME = column_name_param
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE tb_monthly_plan_item ADD COLUMN ', column_ddl_param);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_monthly_plan_item_index_if_missing_v18(
    IN index_name_param VARCHAR(64),
    IN index_ddl_param TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tb_monthly_plan_item'
          AND INDEX_NAME = index_name_param
    ) THEN
        SET @ddl = index_ddl_param;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE add_monthly_plan_item_fk_if_missing_v18(
    IN constraint_name_param VARCHAR(64),
    IN fk_ddl_param TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tb_monthly_plan_item'
          AND CONSTRAINT_NAME = constraint_name_param
    ) THEN
        SET @ddl = fk_ddl_param;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_monthly_plan_item_column_if_missing_v18('recurring_template_id', 'recurring_template_id BIGINT NULL AFTER recurring');
CALL add_monthly_plan_item_column_if_missing_v18('generated_from_item_id', 'generated_from_item_id BIGINT NULL AFTER recurring_template_id');
CALL add_monthly_plan_item_column_if_missing_v18('recurrence_key', 'recurrence_key VARCHAR(120) NULL AFTER generated_from_item_id');
CALL add_monthly_plan_item_column_if_missing_v18('recurrence_modified_manually', 'recurrence_modified_manually BOOLEAN NOT NULL DEFAULT FALSE AFTER recurrence_key');

UPDATE tb_monthly_plan_item
SET recurrence_modified_manually = FALSE
WHERE recurrence_modified_manually IS NULL;

UPDATE tb_monthly_plan_item
SET recurring_template_id = id
WHERE recurring = TRUE
  AND aggregation_type <> 'GROUP_CHILD'
  AND parent_item_id IS NULL
  AND recurring_template_id IS NULL;

UPDATE tb_monthly_plan_item
SET recurrence_key = CONCAT('monthly-plan-item:', recurring_template_id, ':period:', financial_period_id)
WHERE recurring = TRUE
  AND aggregation_type <> 'GROUP_CHILD'
  AND parent_item_id IS NULL
  AND recurring_template_id IS NOT NULL
  AND (recurrence_key IS NULL OR recurrence_key = '');

CALL add_monthly_plan_item_index_if_missing_v18(
    'idx_monthly_plan_item_rec_template',
    'CREATE INDEX idx_monthly_plan_item_rec_template ON tb_monthly_plan_item (recurring_template_id)'
);

CALL add_monthly_plan_item_index_if_missing_v18(
    'idx_monthly_plan_item_generated_from',
    'CREATE INDEX idx_monthly_plan_item_generated_from ON tb_monthly_plan_item (generated_from_item_id)'
);

CALL add_monthly_plan_item_index_if_missing_v18(
    'idx_monthly_plan_item_rec_key',
    'CREATE INDEX idx_monthly_plan_item_rec_key ON tb_monthly_plan_item (recurrence_key)'
);

CALL add_monthly_plan_item_index_if_missing_v18(
    'uk_monthly_plan_item_owner_rec_key',
    'CREATE UNIQUE INDEX uk_monthly_plan_item_owner_rec_key ON tb_monthly_plan_item (owner_id, recurrence_key)'
);

CALL add_monthly_plan_item_fk_if_missing_v18(
    'fk_monthly_plan_item_rec_template',
    'ALTER TABLE tb_monthly_plan_item ADD CONSTRAINT fk_monthly_plan_item_rec_template FOREIGN KEY (recurring_template_id) REFERENCES tb_monthly_plan_item (id)'
);

CALL add_monthly_plan_item_fk_if_missing_v18(
    'fk_monthly_plan_item_generated_from',
    'ALTER TABLE tb_monthly_plan_item ADD CONSTRAINT fk_monthly_plan_item_generated_from FOREIGN KEY (generated_from_item_id) REFERENCES tb_monthly_plan_item (id)'
);

DROP PROCEDURE add_monthly_plan_item_column_if_missing_v18;
DROP PROCEDURE add_monthly_plan_item_index_if_missing_v18;
DROP PROCEDURE add_monthly_plan_item_fk_if_missing_v18;
