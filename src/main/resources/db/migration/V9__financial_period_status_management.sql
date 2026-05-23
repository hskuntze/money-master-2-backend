-- Introduz o status SCHEDULED e corrige casos gerados pela virada antecipada.
-- Regra nova:
--   OPEN      = ciclo ativo/editável do momento
--   SCHEDULED = próximo ciclo já criado/agendado, mas ainda não iniciado
--   CLOSED    = ciclo encerrado e sem pendências

UPDATE tb_financial_period
SET status = 'SCHEDULED',
    closed_at = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'OPEN'
  AND start_date > CURRENT_DATE();

UPDATE tb_financial_period p
SET p.status = 'OPEN',
    p.closed_at = NULL,
    p.updated_at = CURRENT_TIMESTAMP
WHERE p.status = 'CLOSED'
  AND p.start_date <= CURRENT_DATE()
  AND p.end_date >= CURRENT_DATE()
  AND EXISTS (
      SELECT 1
      FROM tb_monthly_plan_item i
      WHERE i.financial_period_id = p.id
        AND i.status IN ('PENDING', 'PARTIALLY_PAID')
  );
