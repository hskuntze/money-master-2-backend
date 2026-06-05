ALTER TABLE tb_installment_purchase
    ADD COLUMN payment_mode VARCHAR(30) NOT NULL DEFAULT 'DIRECT_PAYABLE',
    ADD COLUMN credit_card_id BIGINT NULL,
    ADD COLUMN first_invoice_id BIGINT NULL,
    ADD CONSTRAINT fk_installment_purchase_credit_card FOREIGN KEY (credit_card_id) REFERENCES tb_credit_card (id),
    ADD CONSTRAINT fk_installment_purchase_first_invoice FOREIGN KEY (first_invoice_id) REFERENCES tb_credit_card_invoice (id);

CREATE INDEX idx_installment_purchase_payment_mode ON tb_installment_purchase (payment_mode);
CREATE INDEX idx_installment_purchase_credit_card ON tb_installment_purchase (credit_card_id);
CREATE INDEX idx_installment_purchase_first_invoice ON tb_installment_purchase (first_invoice_id);

ALTER TABLE tb_installment_purchase_entry
    ADD COLUMN invoice_item_id BIGINT NULL,
    ADD COLUMN anticipated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN anticipated_at TIMESTAMP NULL,
    ADD CONSTRAINT fk_installment_entry_invoice_item FOREIGN KEY (invoice_item_id) REFERENCES tb_credit_card_invoice_item (id);

CREATE INDEX idx_installment_entry_invoice_item ON tb_installment_purchase_entry (invoice_item_id);
CREATE INDEX idx_installment_entry_anticipated ON tb_installment_purchase_entry (anticipated);
