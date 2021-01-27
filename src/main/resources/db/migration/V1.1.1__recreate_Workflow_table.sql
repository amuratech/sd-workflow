DROP TABLE IF EXISTS workflow_edit_property_action CASCADE;
DROP TABLE IF EXISTS edit_property_action CASCADE;
DROP TABLE IF EXISTS abstract_workflow_action CASCADE;
DROP TABLE IF EXISTS workflow_trigger CASCADE;
DROP TABLE IF EXISTS workflow_condition CASCADE;
DROP TABLE IF EXISTS workflow CASCADE;


CREATE TABLE workflow
(
  id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(2048),
  entity_type VARCHAR(64) NOT NULL,

  last_triggered_at TIMESTAMP without time zone,
  trigger_count BIGINT NOT NULL DEFAULT 0,

  tenant_id   BIGINT                      NOT NULL,
  created_by  BIGINT                      NOT NULL,
  created_at  TIMESTAMP without time zone NOT NULL,
  updated_by  BIGINT                      NOT NULL,
  updated_at  TIMESTAMP without time zone NOT NULL,

  FOREIGN KEY (created_by) REFERENCES users(id)
);


CREATE TABLE workflow_trigger(
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  trigger_type VARCHAR(255) NOT NULL,
  trigger_frequency VARCHAR(255) NOT NULL,
  workflow_id BIGINT NOT NULL,
  FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);

CREATE TABLE workflow_condition(
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type VARCHAR(255) NOT NULL,
  workflow_id BIGINT NOT NULL,
  FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);


CREATE TABLE abstract_workflow_action(
  id UUID PRIMARY KEY,
  workflow_id BIGINT NOT NULL,
  FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);

CREATE TABLE edit_property_action(
  id UUID PRIMARY KEY,
  workflow_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  value VARCHAR(255) NOT NULL,
  FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);
