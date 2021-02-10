DROP TABLE IF EXISTS create_task_action CASCADE;

CREATE TABLE IF NOT EXISTS create_task_action(
  id UUID PRIMARY KEY,
  workflow_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(2048),
  priority BIGINT,
  outcome VARCHAR(2048),
  type BIGINT NOT NULL,
  status BIGINT NOT NULL,
  assigned_to BIGINT,
  due_days INT NOT NULL,
  due_hours INT NOT NULL,

  FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);