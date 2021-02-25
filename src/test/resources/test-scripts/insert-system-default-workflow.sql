
INSERT INTO users(id, tenant_id, name)
VALUES (12, 99, 'Steve');

INSERT INTO workflow
(id, name, description, entity_type,tenant_id, created_by, created_at, updated_by, updated_at, system_default)
OVERRIDING SYSTEM VALUE VALUES
(4000, 'System-default-workflow', 'SysDefault workflow', 'LEAD', 99, 12, now(), 12, now(), true);

INSERT INTO abstract_workflow_action(id, workflow_id)
VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a85',4000);

INSERT INTO webhook_action (id, workflow_id, name, description, method, authorization_type, request_url)
VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a85',4000,'LastNameWebhook','Last name by webhook', 'GET', 'NONE', 'https://webhook.site/3e0d9676-ad3c-4cf2-a449-ca334e43b815');

INSERT INTO parameter (id, name, entity, attribute, webhook_action_id)
OVERRIDING SYSTEM VALUE VALUES
(1, 'leadName', 'LEAD', 'firstName', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a85');