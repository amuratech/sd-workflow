DELETE FROM workflow_executed_event;
DELETE FROM edit_property_action;
DELETE FROM parameter;
DELETE FROM webhook_action;
DELETE FROM reassign_action;
DELETE FROM abstract_workflow_action;
DELETE FROM workflow_condition;
DELETE FROM workflow_trigger;
DELETE FROM workflow;
DELETE FROM users;

INSERT INTO users(id, tenant_id, name)
VALUES (12, 99, 'Steve'),
(15, 75, 'user 23');


INSERT INTO workflow
(id, name, description, entity_type,
tenant_id, created_by, created_at, updated_by, updated_at )
OVERRIDING SYSTEM VALUE VALUES
(301, 'Workflow 1', 'Workflow 1', 'CONTACT',
99, 12, now(), 12, now()),

(302, 'Workflow 3', 'Workflow 3', 'CONTACT',
75, 15, now(), 15, now());

INSERT INTO workflow_trigger (id, trigger_type, trigger_frequency, workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(101, 'EVENT', 'CREATED', 301),
(102, 'EVENT', 'CREATED', 302);

INSERT INTO workflow_condition (id, type,workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(201, 'FOR_ALL', 301),
(202, 'FOR_ALL', 302);

INSERT INTO abstract_workflow_action(id, workflow_id)
VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',301),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',302);

INSERT INTO edit_property_action (id, workflow_id,name, value, value_type)
VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',301,'firstName','Tony 301', 'PLAIN'),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',302,'firstName','Tony 302', 'PLAIN');


INSERT INTO workflow_executed_event(id, workflow_id, last_triggered_at, trigger_count)
OVERRIDING SYSTEM VALUE VALUES
(401, 301, null, 151),
(402, 302, '2020-10-21 04:47:59.442', 15);
