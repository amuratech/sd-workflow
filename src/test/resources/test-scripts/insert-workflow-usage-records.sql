DELETE FROM parameter;
DELETE FROM webhook_action;
DELETE FROM reassign_action;
DELETE FROM edit_property_action;
DELETE FROM workflow_executed_event;
DELETE FROM abstract_workflow_action;
DELETE FROM workflow_condition;
DELETE FROM workflow_trigger;
DELETE FROM workflow;
DELETE FROM users;

INSERT INTO users(id, tenant_id, name) VALUES
(12, 99, 'Steve'),
(15, 75, 'user 23');

INSERT INTO workflow
(id, name, description, entity_type, tenant_id, created_by, created_at, updated_by, updated_at, active)
OVERRIDING SYSTEM VALUE VALUES
(301, 'Workflow 1', 'Workflow 1', 'LEAD',99, 12, now(), 12, now(), TRUE),

(303, 'Workflow 3', 'Workflow 3', 'LEAD',75, 15, now(), 75, now(), TRUE),
(304, 'Workflow 4', 'Workflow 4', 'LEAD',75, 15, now(), 15, now(), TRUE),
(305, 'Workflow 5', 'Workflow 5', 'DEAL',75, 15, now(), 12, now(), FALSE);