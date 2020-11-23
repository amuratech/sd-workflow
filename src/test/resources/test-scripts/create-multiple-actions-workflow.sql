DELETE FROM workflow_executed_event;
DELETE FROM edit_property_action;
DELETE FROM abstract_workflow_action;
DELETE FROM workflow_condition;
DELETE FROM workflow_trigger;
DELETE FROM workflow;
DELETE FROM users;

INSERT INTO users(id, tenant_id, name)VALUES
(12, 99, 'Steve'),
(15, 75, 'user 23');


INSERT INTO workflow
(id, name, description, entity_type,
tenant_id, created_by, created_at, updated_by, updated_at )
OVERRIDING SYSTEM VALUE VALUES
(301, 'Workflow 1', 'Workflow 1', 'LEAD', 99, 12, now(), 12, now());

INSERT INTO workflow_trigger (id, trigger_type, trigger_frequency, workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(101, 'EVENT', 'CREATED', 301);

INSERT INTO workflow_condition (id, type,workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(201, 'FOR_ALL', 301);

INSERT INTO abstract_workflow_action(id, workflow_id)
VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',301),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',301);

INSERT INTO edit_property_action (id, workflow_id,name, value) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',301,'firstName','Tony 301');

INSERT INTO webhook_action (id, workflow_id, name, description, method, authorization_type, request_url) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',301, 'webhookName', 'webhook desc', 'GET', 'NONE', 'some-request-url');

INSERT INTO parameter (id,name,entity, attribute, webhook_action_id) OVERRIDING SYSTEM VALUE VALUES
(2000, 'param1','Lead', 'firstName', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12');

INSERT INTO workflow_executed_event(id, workflow_id, last_triggered_at, trigger_count)
OVERRIDING SYSTEM VALUE VALUES
(401, 301, null, 151);
