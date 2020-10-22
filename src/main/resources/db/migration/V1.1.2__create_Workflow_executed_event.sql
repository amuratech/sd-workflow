DROP TABLE IF EXISTS workflow_executed_event;

CREATE TABLE workflow_executed_event(
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  last_triggered_at TIMESTAMP without time zone,
  trigger_count BIGINT NOT NULL DEFAULT 0,
  workflow_id BIGINT NOT NULL,
  FOREIGN KEY (workflow_id) REFERENCES workflow(id)
);

INSERT INTO workflow_executed_event (last_triggered_at,trigger_count, workflow_id)
SELECT now(),0,id from workflow;