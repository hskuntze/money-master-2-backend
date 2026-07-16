-- Garante que uma transacao real nao possa ser usada por mais de um item de fatura.
-- Se ja houver duplicidade historica, a migration para para permitir correcao auditavel antes de criar a constraint.

DELIMITER $$

CREATE PROCEDURE add_invoice_item_transaction_unique_index_if_safe()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tb_credit_card_invoice_item'
          AND INDEX_NAME = 'uk_invoice_item_transaction'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM tb_credit_card_invoice_item
            WHERE transaction_id IS NOT NULL
            GROUP BY transaction_id
            HAVING COUNT(*) > 1
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Existem transacoes vinculadas a mais de um item de fatura. Corrija os vinculos antes de aplicar V26.';
        ELSE
            ALTER TABLE tb_credit_card_invoice_item
                ADD UNIQUE INDEX uk_invoice_item_transaction (transaction_id);
        END IF;
    END IF;
END$$

DELIMITER ;

CALL add_invoice_item_transaction_unique_index_if_safe();
DROP PROCEDURE add_invoice_item_transaction_unique_index_if_safe;
