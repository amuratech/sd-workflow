DELETE FROM parameter;
DELETE FROM webhook_action;
DELETE FROM reassign_action;
DELETE FROM edit_property_action;
DELETE FROM create_task_action;
DELETE FROM workflow_executed_event;
DELETE FROM abstract_workflow_action;
DELETE FROM workflow_condition;
DELETE FROM workflow_trigger;
DELETE FROM workflow;
DELETE FROM users;

INSERT INTO users(id, tenant_id, name)
VALUES (12, 55, 'Steve');


INSERT INTO workflow
(id, name, description, entity_type,
tenant_id, created_by, created_at, updated_by, updated_at )
OVERRIDING SYSTEM VALUE VALUES
(301, 'Workflow 1', 'Workflow 1', 'LEAD',
55, 12, '2020-10-20T23:17:59.442', 12, '2020-10-20T23:17:59.442'),

(302, 'Workflow 2', 'Workflow 2', 'LEAD',
55, 12, '2020-10-20T23:17:59.442', 12, '2020-10-20T23:17:59.442');


INSERT INTO workflow_trigger (id, trigger_type, trigger_frequency, workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(101, 'EVENT', 'CREATED', 301),
(102, 'EVENT', 'CREATED', 302);

INSERT INTO workflow_condition (id, type,workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(201, 'FOR_ALL', 301),
(202, 'FOR_ALL', 302);

INSERT INTO abstract_workflow_action(id, workflow_id)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 302);

INSERT INTO edit_property_action(id, workflow_id, name, value, value_type)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301, 'firstName', 'Tony 301', 'PLAIN'),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 302, 'firstName', 'Tony 302', 'PLAIN');

INSERT INTO reassign_action(id, workflow_id, owner_id, name)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301, 20003, 'Tony Stark'),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301, 20003,'Tony Stark');

INSERT INTO create_task_action(id, workflow_id, name, description, priority, outcome, type, status, assigned_to_type, assigned_to_id, assigned_to_name, due_days, due_hours)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301, 'new task', 'new task desc', 2, 'contacted', 3, 4, 'USER', 5, 'James Bond', 4, 2);

INSERT INTO workflow_executed_event(id, workflow_id, last_triggered_at, trigger_count)
OVERRIDING SYSTEM VALUE VALUES
(401, 301, '2020-10-21T23:17:59.442', 151),
(402, 302, '2020-10-20T23:17:59.442', 1);
