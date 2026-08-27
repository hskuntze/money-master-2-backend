-- Diferencia itens previstos de cobrancas confirmadas na composicao da fatura.
-- Compatibilidade: itens historicos permanecem como CONFIRMED, pois antes nao havia estado separado.

DELIMITER $$

CREATE PROCEDURE add_invoice_item_confirmation_status_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tb_credit_card_invoice_item'
          AND COLUMN_NAME = 'confirmation_status'
    ) THEN
        ALTER TABLE tb_credit_card_invoice_item
            ADD COLUMN confirmation_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED' AFTER source_type;
    END IF;

    UPDATE tb_credit_card_invoice_item
    SET confirmation_status = 'CONFIRMED'
    WHERE confirmation_status IS NULL OR confirmation_status = '';

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tb_credit_card_invoice_item'
          AND INDEX_NAME = 'idx_invoice_item_confirmation_status'
    ) THEN
        CREATE INDEX idx_invoice_item_confirmation_status
            ON tb_credit_card_invoice_item (confirmation_status);
    END IF;
END$$

DELIMITER ;

CALL add_invoice_item_confirmation_status_if_missing();
DROP PROCEDURE add_invoice_item_confirmation_status_if_missing;
