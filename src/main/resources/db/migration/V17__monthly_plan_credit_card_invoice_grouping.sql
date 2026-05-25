-- Suporte incremental a fatura manual de cartão com agrupamento visual de parcelas.
-- Migration segura: adiciona campos, índices e FK sem apagar dados existentes.

DELIMITER $$

CREATE PROCEDURE add_monthly_plan_item_column_if_missing(
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

CREATE PROCEDURE add_monthly_plan_item_index_if_missing(
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

CREATE PROCEDURE add_monthly_plan_item_fk_if_missing(
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

CALL add_monthly_plan_item_column_if_missing('parent_item_id', 'parent_item_id BIGINT NULL AFTER financial_period_id');
CALL add_monthly_plan_item_column_if_missing('aggregation_type', 'aggregation_type VARCHAR(30) NOT NULL DEFAULT ''NORMAL'' AFTER nature');
CALL add_monthly_plan_item_column_if_missing('settlement_origin', 'settlement_origin VARCHAR(30) NOT NULL DEFAULT ''DIRECT'' AFTER aggregation_type');
CALL add_monthly_plan_item_column_if_missing('paid_by_parent', 'paid_by_parent BOOLEAN NOT NULL DEFAULT FALSE AFTER settlement_origin');

UPDATE tb_monthly_plan_item
SET aggregation_type = 'NORMAL'
WHERE aggregation_type IS NULL OR aggregation_type = '';

UPDATE tb_monthly_plan_item
SET settlement_origin = 'DIRECT'
WHERE settlement_origin IS NULL OR settlement_origin = '';

UPDATE tb_monthly_plan_item
SET paid_by_parent = FALSE
WHERE paid_by_parent IS NULL;

CALL add_monthly_plan_item_index_if_missing(
    'idx_monthly_plan_item_parent',
    'CREATE INDEX idx_monthly_plan_item_parent ON tb_monthly_plan_item (parent_item_id)'
);

CALL add_monthly_plan_item_index_if_missing(
    'idx_monthly_plan_item_aggregation',
    'CREATE INDEX idx_monthly_plan_item_aggregation ON tb_monthly_plan_item (aggregation_type)'
);

CALL add_monthly_plan_item_fk_if_missing(
    'fk_monthly_plan_item_parent',
    'ALTER TABLE tb_monthly_plan_item ADD CONSTRAINT fk_monthly_plan_item_parent FOREIGN KEY (parent_item_id) REFERENCES tb_monthly_plan_item (id)'
);

DROP PROCEDURE add_monthly_plan_item_column_if_missing;
DROP PROCEDURE add_monthly_plan_item_index_if_missing;
DROP PROCEDURE add_monthly_plan_item_fk_if_missing;
