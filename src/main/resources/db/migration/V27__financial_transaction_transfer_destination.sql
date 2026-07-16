-- Representa estruturalmente a conta de destino em transferencias entre contas.
-- A coluna e opcional para preservar lancamentos historicos existentes.

ALTER TABLE tb_financial_transaction
    ADD COLUMN destination_account_id BIGINT NULL AFTER account_id;

ALTER TABLE tb_financial_transaction
    ADD CONSTRAINT fk_transaction_destination_account
        FOREIGN KEY (destination_account_id) REFERENCES tb_account (id);

CREATE INDEX idx_transaction_destination_account
    ON tb_financial_transaction (destination_account_id);
