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

INSERT INTO users(id, tenant_id, name)
VALUES (11, 99, 'Tony Stark');
INSERT INTO users(id, tenant_id, name)
VALUES (12, 99, 'Steve');

INSERT INTO users(id, tenant_id, name)
VALUES (15, 75, 'user 23');
