INSERT INTO users(id, tenant_id, name)
VALUES
(12, 99, 'Steve'),
(15, 75, 'user 23');


INSERT INTO workflow
(id, name, description, entity_type, active,
tenant_id, created_by, created_at, updated_by, updated_at )
OVERRIDING SYSTEM VALUE VALUES
(301, 'Workflow 1', 'Workflow 1', 'LEAD', false,
99, 12, now(), 12, now()),
(302, 'Workflow 2', 'Workflow 2', 'LEAD', true,
99, 12, now(), 12, now());

INSERT INTO workflow_trigger (id, trigger_type, trigger_frequency, workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(101, 'EVENT', 'CREATED', 301),
(102, 'EVENT', 'CREATED', 302);

INSERT INTO workflow_condition (id, type,workflow_id)
OVERRIDING SYSTEM VALUE VALUES
(201, 'FOR_ALL', 301),
(202, 'FOR_ALL', 302);