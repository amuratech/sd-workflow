DROP TABLE IF EXISTS reassign_action CASCADE;

CREATE TABLE IF NOT EXISTS reassign_action(
  id UUID PRIMARY KEY,
  workflow_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,

  FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);