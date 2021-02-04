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
VALUES (12, 55, 'Steve'),
(11, 99, 'user 11');


INSERT INTO workflow
(id, name, description, entity_type,
tenant_id, created_by, created_at, updated_by, updated_at )
OVERRIDING SYSTEM VALUE VALUES
(301, 'Workflow 1', 'Workflow 1', 'DEAL',
55, 12, '2020-10-20T23:17:59.442', 12, '2020-10-20T23:17:59.442'),

(302, 'Workflow 2', 'Workflow 2', 'DEAL',
99, 11, '2020-10-20T23:17:59.442', 11, '2020-10-20T23:17:59.442');


INSERT INTO workflow_trigger (id, trigger_type, trigger_frequency, workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(101, 'EVENT', 'UPDATED', 301),
(102, 'EVENT', 'UPDATED', 302);

INSERT INTO workflow_condition (id, type,workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(201, 'FOR_ALL', 301),
(202, 'FOR_ALL', 302);

INSERT INTO abstract_workflow_action(id, workflow_id)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 302);

INSERT INTO edit_property_action(id, workflow_id, name, value, value_type)
VALUES ('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a12', 301, 'ownedBy', '{"id": 12,"name": "James Bond"}', 'OBJECT'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 301, 'name', 'updated deal', 'PLAIN'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a13', 301, 'estimatedValue', '{"currencyId": 1, "value": 1000}', 'OBJECT'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a14', 301, 'actualValue', '{"currencyId": 2, "value": 2000}', 'OBJECT'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a15', 301, 'estimatedClosureOn', '2021-01-14T06:30:00.000Z', 'PLAIN'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a16', 301, 'product', '{"id": 2, "name": "Marketing Service"}', 'OBJECT'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a17', 301, 'pipeline', '{ "id": 122, "name": "Demo", "stage": { "id": 1, "name": "Open" } }', 'OBJECT'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a18', 301, 'associatedContacts', '[ { "id": 14, "name": "Tony Stark" } ]', 'ARRAY'),
('a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a19', 301, 'company', '{"id": 15, "name": "Dell"}', 'OBJECT'),
('b0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11', 302, 'name', 'Tony 302', 'PLAIN');

INSERT INTO workflow_executed_event(id, workflow_id, last_triggered_at, trigger_count)
OVERRIDING SYSTEM VALUE VALUES
(401, 301, null, 151),
(402, 302, '2020-10-20T23:17:59.442', 1);