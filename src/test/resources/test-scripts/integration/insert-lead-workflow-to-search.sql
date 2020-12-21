DELETE FROM edit_property_action;
DELETE FROM workflow_executed_event;
DELETE FROM abstract_workflow_action;
DELETE FROM workflow_condition;
DELETE FROM workflow_trigger;
DELETE FROM workflow;
DELETE FROM users;

INSERT INTO users(id, tenant_id, name)
VALUES
(12, 55, 'Steve'),
(13, 55, 'Roger'),
(15, 75, 'user 23');


INSERT INTO workflow
(id, name, description, entity_type,
tenant_id, created_by, created_at, updated_by, updated_at, active )
OVERRIDING SYSTEM VALUE VALUES
(301, 'Workflow 301', 'Workflow 301', 'LEAD',
55, 12, '2020-10-21T10:53:58.250', 12, '2020-10-21T10:53:58.250',true),

(302, 'Workflow 302', 'Workflow 302', 'LEAD',
55, 13, '2020-10-23T10:53:58.250', 13, '2020-10-23T11:53:58.250',false),

(303, 'Workflow 303', 'Workflow 303', 'LEAD',
55, 13, '2020-10-26T10:53:58.250', 13, '2020-10-26T12:53:58.250',true),


(377, 'Workflow 377', 'Workflow 377', 'LEAD',
75, 15, '2020-10-21T10:53:58.250', 15, '2020-10-21T10:53:58.250',false);

INSERT INTO workflow_executed_event (id,last_triggered_at, trigger_count, workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(55,'2020-02-27T08:58:23.623',50,301),
(56,'2020-03-25T08:58:23.623',40,302),
(57,'2020-03-27T08:58:23.623',60,377);

INSERT INTO workflow_trigger (id, trigger_type, trigger_frequency, workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(101, 'EVENT', 'CREATED', 301),
(102, 'EVENT', 'CREATED', 302),
(103, 'EVENT', 'CREATED', 302),
(104, 'EVENT', 'CREATED', 303);

INSERT INTO workflow_condition (id, type,workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(201, 'FOR_ALL', 301),
(202, 'FOR_ALL', 302),
(203, 'FOR_ALL', 302),
(204, 'FOR_ALL', 303);

INSERT INTO abstract_workflow_action(id, workflow_id)
VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',301),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',302),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',302),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',303);

INSERT INTO edit_property_action (id, workflow_id,name, value, value_type)
VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',301,'firstName','Tony 301','PLAIN'),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',302,'firstName','Tony 302','PLAIN'),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',303,'firstName','Tony 303','PLAIN'),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',377,'firstName','Tony 377','PLAIN');
