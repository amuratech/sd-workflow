DROP TABLE IF EXISTS participant CASCADE;
DROP TABLE IF EXISTS email_action CASCADE;

CREATE TABLE IF NOT EXISTS email_action(
  id UUID PRIMARY KEY,
  workflow_id BIGINT NOT NULL,
  email_template_id BIGINT NOT NULL,
  email_from VARCHAR(1024) NOT NULL,

  FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);

CREATE TABLE IF NOT EXISTS participant(
  id UUID PRIMARY KEY,
  type VARCHAR(64) NOT NULL,
  entity VARCHAR(64) NOT NULL,
  entity_id BIGINT,
  name VARCHAR(255),
  email VARCHAR(255),
  email_action_to_id UUID,
  email_action_cc_id UUID,
  email_action_bcc_id UUID,

  FOREIGN KEY (email_action_to_id) REFERENCES email_action(id),
  FOREIGN KEY (email_action_cc_id) REFERENCES email_action(id),
  FOREIGN KEY (email_action_bcc_id) REFERENCES email_action(id)
);