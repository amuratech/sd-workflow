ALTER TABLE workflow ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true;