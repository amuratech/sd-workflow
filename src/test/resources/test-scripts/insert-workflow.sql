DELETE FROM parameter;
DELETE FROM participant;
DELETE FROM webhook_action;
DELETE FROM reassign_action;
DELETE FROM edit_property_action;
DELETE FROM create_task_action;
DELETE FROM email_action;
DELETE FROM workflow_executed_event;
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
75, 15, now(), 75, now()),

(304, 'Workflow 4', 'Workflow 4', 'LEAD',
75, 15, now(), 15, now()),

(305, 'Workflow 5', 'Workflow 5', 'DEAL',
99, 12, now(), 12, now());

INSERT INTO workflow_trigger (id, trigger_type, trigger_frequency, workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(101, 'EVENT', 'CREATED', 301),
(102, 'EVENT', 'CREATED', 302),
(103, 'EVENT', 'CREATED', 303),
(104, 'EVENT', 'UPDATED', 304),
(105, 'EVENT', 'UPDATED', 305);

INSERT INTO workflow_condition (id, type,workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(201, 'FOR_ALL', 301),
(202, 'FOR_ALL', 302),
(203, 'FOR_ALL', 303),
(204, 'FOR_ALL', 304),
(205, 'FOR_ALL', 305);


INSERT INTO abstract_workflow_action(id, workflow_id)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 302),
('c0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 303),
('d0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 304),
('e0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 305);

INSERT INTO edit_property_action(id, workflow_id, name, value, value_type)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301, 'firstName', 'Tony 301','PLAIN'),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 302, 'firstName', 'Tony 302','PLAIN'),
('c0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 303, 'firstName', 'Tony 303','PLAIN'),
('d0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 304, 'firstName', 'Tony 304','PLAIN'),
('e0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 305, 'name', 'Tony 304','PLAIN'),
('f0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 305, 'product', '{"id":26,"name":"Marketing Service"}','OBJECT'),
('f0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13', 305, 'company', '{"id":26,"name":"Marketing Service"}','OBJECT');

INSERT INTO create_task_action(id, workflow_id, name, description, priority, outcome, type, status, assigned_to_type, assigned_to_id, assigned_to_name, due_days, due_hours)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301, 'new task', 'new task desc', 2, 'contacted', 3, 4, 'USER', 5, 'James Bond', 4, 2),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 305, 'new task', 'new task desc', 2, 'contacted', 3, 4, 'USER', 5, 'James Bond', 4, 2);

INSERT INTO email_action(id, workflow_id, email_template_id, email_from)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13', 301, 2, '{ "type": "RECORD_OWNER", "entity": "user", "entityId": 1, "name": "user1", "email": "user1@gmail.com" }');

INSERT INTO participant(id, type, entity, entity_id, name, email, email_action_to_id, email_action_cc_id, email_action_bcc_id)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a14','RECORD_OWNER', 'lead', 2, 'test', 'test@123.com', 'a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13', 'a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13', 'a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a15','RECORD_OWNER', 'lead', 3, 'test', 'test@123.com','a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13', 'a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13', 'a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a16','RECORD_OWNER', 'lead', 4, 'test', 'test@123.com','a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13', 'a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13', 'a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13');


INSERT INTO workflow_executed_event(id, workflow_id, last_triggered_at, trigger_count)
OVERRIDING SYSTEM VALUE VALUES
(401, 301, null, 151),
(402, 302, '2020-10-21 04:47:59.442', 15),
(403, 303, '2020-10-21 04:47:59.442', 10),
(404, 304, '2020-10-21 04:47:59.442', 10),
(405, 305, null, 10);