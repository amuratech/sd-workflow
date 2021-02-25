ALTER TABLE workflow
  ADD COLUMN IF NOT EXISTS system_default boolean NOT NULL DEFAULT false;