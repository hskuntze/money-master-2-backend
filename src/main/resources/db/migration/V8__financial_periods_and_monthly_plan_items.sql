CREATE TABLE tb_financial_period (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    turnover_day INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    archived_income_total DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    archived_expense_total DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    archived_transfer_total DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    archived_net_total DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_financial_period_owner_start UNIQUE (owner_id, start_date),
    CONSTRAINT fk_financial_period_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_financial_period_owner_status ON tb_financial_period (owner_id, status);
CREATE INDEX idx_financial_period_owner_dates ON tb_financial_period (owner_id, start_date, end_date);

CREATE TABLE tb_monthly_plan_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    financial_period_id BIGINT NOT NULL,
    account_id BIGINT NULL,
    category_id BIGINT NULL,
    type VARCHAR(30) NOT NULL,
    description VARCHAR(255) NOT NULL,
    expected_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    actual_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    due_date DATE NOT NULL,
    paid_on DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    nature VARCHAR(30) NOT NULL DEFAULT 'VARIABLE',
    recurring BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_monthly_plan_item_owner FOREIGN KEY (owner_id) REFERENCES tb_user (id),
    CONSTRAINT fk_monthly_plan_item_period FOREIGN KEY (financial_period_id) REFERENCES tb_financial_period (id),
    CONSTRAINT fk_monthly_plan_item_account FOREIGN KEY (account_id) REFERENCES tb_account (id),
    CONSTRAINT fk_monthly_plan_item_category FOREIGN KEY (category_id) REFERENCES tb_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_monthly_plan_item_owner_period ON tb_monthly_plan_item (owner_id, financial_period_id);
CREATE INDEX idx_monthly_plan_item_status ON tb_monthly_plan_item (status);
CREATE INDEX idx_monthly_plan_item_due_date ON tb_monthly_plan_item (due_date);

ALTER TABLE tb_financial_transaction
    ADD COLUMN financial_period_id BIGINT NULL AFTER category_id,
    ADD COLUMN monthly_plan_item_id BIGINT NULL AFTER financial_period_id;

CREATE INDEX idx_transaction_financial_period ON tb_financial_transaction (financial_period_id);
CREATE INDEX idx_transaction_monthly_plan_item ON tb_financial_transaction (monthly_plan_item_id);

ALTER TABLE tb_financial_transaction
    ADD CONSTRAINT fk_transaction_financial_period FOREIGN KEY (financial_period_id) REFERENCES tb_financial_period (id),
    ADD CONSTRAINT fk_transaction_monthly_plan_item FOREIGN KEY (monthly_plan_item_id) REFERENCES tb_monthly_plan_item (id);

INSERT INTO tb_financial_period (
    owner_id,
    name,
    start_date,
    end_date,
    turnover_day,
    status,
    archived_income_total,
    archived_expense_total,
    archived_transfer_total,
    archived_net_total,
    closed_at,
    created_at
)
SELECT
    periods.owner_id,
    CONCAT('Ciclo iniciado em ', DATE_FORMAT(periods.start_date, '%d/%m/%Y')),
    periods.start_date,
    LAST_DAY(periods.start_date),
    DAYOFMONTH(periods.start_date),
    CASE WHEN periods.start_date = latest.max_start_date THEN 'OPEN' ELSE 'CLOSED' END,
    0.00,
    0.00,
    0.00,
    0.00,
    CASE WHEN periods.start_date = latest.max_start_date THEN NULL ELSE CURRENT_TIMESTAMP END,
    CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT
        owner_id,
        DATE_SUB(occurred_on, INTERVAL DAYOFMONTH(occurred_on) - 1 DAY) AS start_date
    FROM tb_financial_transaction
) periods
JOIN (
    SELECT
        owner_id,
        MAX(DATE_SUB(occurred_on, INTERVAL DAYOFMONTH(occurred_on) - 1 DAY)) AS max_start_date
    FROM tb_financial_transaction
    GROUP BY owner_id
) latest ON latest.owner_id = periods.owner_id;

UPDATE tb_financial_transaction t
JOIN tb_financial_period p
  ON p.owner_id = t.owner_id
 AND t.occurred_on BETWEEN p.start_date AND p.end_date
SET t.financial_period_id = p.id
WHERE t.financial_period_id IS NULL;

UPDATE tb_financial_period p
LEFT JOIN (
    SELECT
        financial_period_id,
        SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) AS income_total,
        SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS expense_total,
        SUM(CASE WHEN type = 'TRANSFER' THEN amount ELSE 0 END) AS transfer_total
    FROM tb_financial_transaction
    WHERE financial_period_id IS NOT NULL
    GROUP BY financial_period_id
) totals ON totals.financial_period_id = p.id
SET
    p.archived_income_total = COALESCE(totals.income_total, 0.00),
    p.archived_expense_total = COALESCE(totals.expense_total, 0.00),
    p.archived_transfer_total = COALESCE(totals.transfer_total, 0.00),
    p.archived_net_total = COALESCE(totals.income_total, 0.00) - COALESCE(totals.expense_total, 0.00)
WHERE p.status = 'CLOSED';
