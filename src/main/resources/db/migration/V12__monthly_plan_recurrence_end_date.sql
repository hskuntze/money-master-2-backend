ALTER TABLE tb_monthly_plan_item
    ADD COLUMN recurrence_end_date DATE NULL AFTER recurring;
