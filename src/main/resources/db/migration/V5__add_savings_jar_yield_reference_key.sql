ALTER TABLE tb_savings_jar_movement
    ADD COLUMN reference_key VARCHAR(80) NULL AFTER rate_reference;

CREATE UNIQUE INDEX uk_savings_jar_movement_reference_key
    ON tb_savings_jar_movement (savings_jar_id, reference_key);
