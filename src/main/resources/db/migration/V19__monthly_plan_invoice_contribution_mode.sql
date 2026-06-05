-- Diferencia composição retroativa da fatura e lançamento novo que aumenta o total.
-- Migration segura: preserva faturas antigas e assume filhos existentes como composição informativa.

DELIMITER $$

CREATE PROCEDURE add_monthly_plan_item_column_if_missing_v19(
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

CREATE PROCEDURE add_monthly_plan_item_index_if_missing_v19(
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

DELIMITER ;

CALL add_monthly_plan_item_column_if_missing_v19(
    'invoice_base_amount',
    'invoice_base_amount DECIMAL(15,2) NULL AFTER actual_amount'
);

CALL add_monthly_plan_item_column_if_missing_v19(
    'invoice_contribution_mode',
    'invoice_contribution_mode VARCHAR(30) NOT NULL DEFAULT ''COMPOSITION_ONLY'' AFTER paid_by_parent'
);

UPDATE tb_monthly_plan_item
SET invoice_contribution_mode = 'COMPOSITION_ONLY'
WHERE invoice_contribution_mode IS NULL OR invoice_contribution_mode = '';

UPDATE tb_monthly_plan_item
SET invoice_base_amount = expected_amount
WHERE aggregation_type = 'GROUP_PARENT'
  AND invoice_base_amount IS NULL;

UPDATE tb_monthly_plan_item
SET invoice_base_amount = NULL
WHERE aggregation_type <> 'GROUP_PARENT';

CALL add_monthly_plan_item_index_if_missing_v19(
    'idx_monthly_plan_item_invoice_mode',
    'CREATE INDEX idx_monthly_plan_item_invoice_mode ON tb_monthly_plan_item (invoice_contribution_mode)'
);

DROP PROCEDURE add_monthly_plan_item_column_if_missing_v19;
DROP PROCEDURE add_monthly_plan_item_index_if_missing_v19;
