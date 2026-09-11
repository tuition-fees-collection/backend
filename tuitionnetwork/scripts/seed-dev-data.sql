
INSERT INTO identity.role (id, name, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'TELLER', 'Branch officer who takes payments')
ON CONFLICT (name) DO NOTHING;

INSERT INTO identity.backoffice_employee (id, full_name, branch, role_id) VALUES
    ('22222222-2222-2222-2222-222222222222', 'Mohamed Hamouda', 'Giza Main',
     '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;
INSERT INTO identity.backoffice_employee (id, full_name, branch, role_id) VALUES
    ('22222222-2222-2222-2222-000000000000', 'ONLINE CHANNEL', 'SYSTEM',
     '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;
INSERT INTO identity.guardian (id, national_id_hmac, national_id_encrypted, full_name, verified) VALUES
    ('33333333-3333-3333-3333-333333333333', 'hmac-dev-guardian-1',
     '\x00'::bytea, 'Mona Samir Abdelrahman', TRUE)
ON CONFLICT (national_id_hmac) DO NOTHING;

INSERT INTO identity.student (id, national_id_hmac, full_name, date_of_birth) VALUES
    ('44444444-4444-4444-4444-444444444444', 'hmac-dev-student-1', 'Nour Khaled Fahmy', '2012-03-22')
ON CONFLICT (national_id_hmac) DO NOTHING;

INSERT INTO identity.guardian_student (guardian_id, student_id, relationship, source) VALUES
    ('33333333-3333-3333-3333-333333333333', '44444444-4444-4444-4444-444444444444',
     'MOTHER', 'INSTITUTION_IMPORT')
ON CONFLICT (guardian_id, student_id) DO NOTHING;
INSERT INTO identity.student (id, national_id_hmac, full_name, date_of_birth) VALUES
    ('44444444-4444-4444-4444-000000000000', 'hmac-dev-student-2', 'Other Family Child', '2011-01-01')
ON CONFLICT (national_id_hmac) DO NOTHING;
INSERT INTO billing.institution (id, name, type, status) VALUES
    ('55555555-5555-5555-5555-555555555555', 'Giza International School', 'SCHOOL', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO billing.fee
    (id, student_id, institution_id, fee_type, collection_period, currency, amount, amount_paid, status)
VALUES
    ('66666666-6666-6666-6666-666666666666', '44444444-4444-4444-4444-444444444444',
     '55555555-5555-5555-5555-555555555555', 'TUITION', '2026-T1', 'EGP',
     18000.00, 8000.00, 'PARTIALLY_PAID'),
    ('77777777-7777-7777-7777-777777777777', '44444444-4444-4444-4444-444444444444',
     '55555555-5555-5555-5555-555555555555', 'BUS', '2026-T1', 'EGP',
     3500.00, 0.00, 'OUTSTANDING'),
    ('88888888-8888-8888-8888-888888888888', '44444444-4444-4444-4444-000000000000',
     '55555555-5555-5555-5555-555555555555', 'TUITION', '2026-T1', 'EGP',
     12000.00, 0.00, 'OUTSTANDING'),
    ('99999999-9999-9999-9999-999999999999', '44444444-4444-4444-4444-444444444444',
     '55555555-5555-5555-5555-555555555555', 'BOOKS', '2025-T2', 'EGP',
     1500.00, 1500.00, 'SETTLED')
ON CONFLICT (id) DO NOTHING;

