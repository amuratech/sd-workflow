ALTER TABLE reassign_action
   DROP COLUMN IF EXISTS name;

ALTER TABLE reassign_action
  ADD COLUMN IF NOT EXISTS name VARCHAR(255);