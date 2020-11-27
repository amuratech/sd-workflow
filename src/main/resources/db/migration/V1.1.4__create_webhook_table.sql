DROP TABLE IF EXISTS parameter CASCADE;
DROP TABLE IF EXISTS webhook_action CASCADE;

CREATE TABLE IF NOT EXISTS webhook_action(
  id UUID PRIMARY KEY,
  workflow_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(2048),
  method VARCHAR(64) NOT NULL,
  authorization_type VARCHAR(64) NOT NULL,
  request_url VARCHAR(2048) NOT NULL,

  FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);

CREATE TABLE IF NOT EXISTS parameter(
  id BIGINT GENERATED ALWAYS AS IDENTITY,
  name VARCHAR(255) NOT NULL,
  entity VARCHAR(255) NOT NULL,
  attribute VARCHAR(255) NOT NULL,
  webhook_action_id UUID,

  PRIMARY KEY (id),
  FOREIGN KEY (webhook_action_id) REFERENCES webhook_action(id)
);
