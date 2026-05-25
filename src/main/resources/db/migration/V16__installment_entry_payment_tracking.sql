-- Controle individual de baixa de parcelas de compras parceladas.
-- Migration segura: apenas adiciona metadados de pagamento e preserva registros existentes.

ALTER TABLE tb_installment_purchase_entry
    ADD COLUMN paid_on DATE NULL AFTER status,
    ADD COLUMN payment_source VARCHAR(30) NOT NULL DEFAULT 'NONE' AFTER paid_on,
    ADD COLUMN paid_by_id BIGINT NULL AFTER payment_source,
    ADD COLUMN payment_registered_at TIMESTAMP NULL AFTER paid_by_id;

ALTER TABLE tb_installment_purchase_entry
    ADD CONSTRAINT fk_installment_entry_paid_by FOREIGN KEY (paid_by_id) REFERENCES tb_user (id);

CREATE INDEX idx_installment_entry_status_source ON tb_installment_purchase_entry (status, payment_source);
CREATE INDEX idx_installment_entry_paid_by ON tb_installment_purchase_entry (paid_by_id);

UPDATE tb_installment_purchase_entry
SET paid_on = COALESCE(paid_on, due_date),
    payment_source = CASE
        WHEN payment_source IS NULL OR payment_source = 'NONE' THEN 'MANUAL'
        ELSE payment_source
    END,
    payment_registered_at = COALESCE(payment_registered_at, updated_at, created_at, CURRENT_TIMESTAMP)
WHERE status = 'PAID';

UPDATE tb_installment_purchase_entry
SET payment_source = 'NONE'
WHERE status <> 'PAID'
  AND (payment_source IS NULL OR payment_source = '');
