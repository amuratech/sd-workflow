ALTER TABLE workflow ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true;

ALTER TABLE edit_property_action ALTER COLUMN value SET DATA TYPE TEXT;