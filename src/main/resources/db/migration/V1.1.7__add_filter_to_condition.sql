ALTER TABLE workflow_condition
  DROP COLUMN IF EXISTS expression;

ALTER TABLE workflow_condition
  ADD COLUMN IF NOT EXISTS expression jsonb;