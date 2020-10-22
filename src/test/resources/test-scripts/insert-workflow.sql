DELETE FROM workflow_executed_event;
DELETE FROM edit_property_action;
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
(301, 'Workflow 1', 'Workflow 1', 'LEAD',
99, 12, now(), 12, now()),
(302, 'Workflow 2', 'Workflow 2', 'LEAD',
99, 12, now(), 12, now()),

(303, 'Workflow 3', 'Workflow 3', 'LEAD',
75, 15, now(), 75, now());

INSERT INTO workflow_trigger (id, trigger_type, trigger_frequency, workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(101, 'EVENT', 'CREATED', 301),
(102, 'EVENT', 'CREATED', 302),
(103, 'EVENT', 'CREATED', 303);

INSERT INTO workflow_condition (id, type,workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(201, 'FOR_ALL', 301),
(202, 'FOR_ALL', 302),
(203, 'FOR_ALL', 303);


INSERT INTO abstract_workflow_action(id, workflow_id)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 302),
('c0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 303);

INSERT INTO edit_property_action(id, workflow_id, name, value)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301, 'firstName', 'Tony 301'),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 302, 'firstName', 'Tony 302'),
('c0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 303, 'firstName', 'Tony 303');


INSERT INTO workflow_executed_event(id, workflow_id, last_triggered_at, trigger_count)
OVERRIDING SYSTEM VALUE VALUES
(401, 301, null, 151),
(402, 302, '2020-10-21 04:47:59.442', 15),
(403, 303, '2020-10-21 04:47:59.442', 10);