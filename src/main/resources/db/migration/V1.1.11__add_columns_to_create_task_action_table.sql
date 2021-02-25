ALTER TABLE create_task_action DROP COLUMN IF EXISTS assigned_to CASCADE;

ALTER TABLE create_task_action
ADD COLUMN IF NOT EXISTS assigned_to_type VARCHAR(32),
ADD COLUMN IF NOT EXISTS assigned_to_id BIGINT,
ADD COLUMN IF NOT EXISTS assigned_to_name VARCHAR(255);