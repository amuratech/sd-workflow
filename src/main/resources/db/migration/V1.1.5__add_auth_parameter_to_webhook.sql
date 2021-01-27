ALTER TABLE webhook_action
  DROP COLUMN IF EXISTS authorization_parameter;

ALTER TABLE webhook_action
  ADD COLUMN IF NOT EXISTS authorization_parameter TEXT;