-- Suporte ao onboarding inicial e ao tour guiado por usuário.
-- A tabela de perfil financeiro já centraliza a personalização do usuário; por isso os novos dados ficam nela.

ALTER TABLE tb_user_financial_profile
    ADD COLUMN preferred_name VARCHAR(80) NULL AFTER profession,
    ADD COLUMN cycle_start_day INT NULL AFTER preferred_name,
    ADD COLUMN income_day INT NULL AFTER cycle_start_day,
    ADD COLUMN initial_goal_target_amount DECIMAL(15,2) NULL AFTER approximate_monthly_income,
    ADD COLUMN onboarding_version VARCHAR(40) NULL AFTER initial_goal_target_amount,
    ADD COLUMN tour_completed BOOLEAN NOT NULL DEFAULT FALSE AFTER onboarding_completed_at,
    ADD COLUMN tour_skipped BOOLEAN NOT NULL DEFAULT FALSE AFTER tour_completed,
    ADD COLUMN tour_completed_at TIMESTAMP NULL AFTER tour_skipped,
    ADD COLUMN tour_skipped_at TIMESTAMP NULL AFTER tour_completed_at,
    ADD COLUMN tour_last_step_key VARCHAR(120) NULL AFTER tour_skipped_at;

CREATE INDEX idx_user_financial_profile_onboarding ON tb_user_financial_profile (owner_id, onboarding_completed);
CREATE INDEX idx_user_financial_profile_tour ON tb_user_financial_profile (owner_id, tour_completed, tour_skipped);
