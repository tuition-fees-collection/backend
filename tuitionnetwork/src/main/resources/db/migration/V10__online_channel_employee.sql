INSERT INTO identity.backoffice_employee (id, full_name, branch, role_id)
SELECT
    '22222222-2222-2222-2222-000000000000',
    'ONLINE CHANNEL',
    'SYSTEM',
    id
FROM identity.role
WHERE name = 'BANK_OFFICER'
ON CONFLICT (id) DO NOTHING;
