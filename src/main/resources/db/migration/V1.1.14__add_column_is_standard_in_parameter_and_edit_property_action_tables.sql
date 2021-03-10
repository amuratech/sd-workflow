ALTER TABLE edit_property_action
DROP COLUMN IF EXISTS is_standard;

ALTER TABLE parameter
DROP COLUMN IF EXISTS is_standard;

ALTER TABLE edit_property_action
ADD COLUMN is_standard boolean DEFAULT TRUE;

ALTER TABLE parameter
ADD COLUMN is_standard boolean DEFAULT TRUE;
