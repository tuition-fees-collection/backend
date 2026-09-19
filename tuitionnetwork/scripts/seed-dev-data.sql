-- Fresh local walkthrough data. Not a Flyway migration.
-- Wipes previous demo rows, then inserts the accounts below.
-- Does not touch identity.role or the ONLINE CHANNEL employee used for payer card payments.
--
--   docker compose exec -T postgres psql -U tuition -d tuition < scripts/seed-dev-data.sql
--
-- Password for every seeded login: Admin123!
-- POST /api/v1/auth/login  {"username":"<user>","password":"Admin123!"}
--
--   nadia     ADMIN                 /backoffice/login     dashboard, create institution/officer
--   karim     BANK_OFFICER          /backoffice/login     find fees, cash pay, client overview
--   laila     INSTITUTION_OFFICER   /institution/login    Nile Valley School (view student receipts, not pay)
--   heba      INSTITUTION_OFFICER   /institution/login    Maadi Language School (no fees yet)
--   mona      PAYER                 API only              own students / pay
--
-- Mock-aligned national IDs (wit-mock-services GET /api/v1/test-data):
--   Parent / lookup / search:  29805150101023  Mona Samir Abdelrahman
--   Student:                   30103222103442  Nour Khaled Fahmy
--   Student:                   31204010102041  Malak Hany Sobhy
--   Frozen lookup (not seeded):29511020204536  Ahmed Tarek Mahmoud
-- Nile Valley School id:       b0b0b0b0-0002-4000-8000-000000000001
-- Reporting date with collections: today (payment settled_at = now())
-- Live CSV after seed:         scripts/sample-fee-upload.csv  (period 2026-10)
--
-- HMAC values match NATIONAL_ID_HMAC_KEY_BASE64 from the README / CI.

BEGIN;

-- ---------------------------------------------------------------------------
-- Remove previous demo data (keep roles + ONLINE CHANNEL employee)
-- ---------------------------------------------------------------------------
DELETE FROM reporting.gateway_check_item;
DELETE FROM reporting.gateway_check_run;
DELETE FROM reporting.daily_reports;
DELETE FROM epp.epp_instalment;
DELETE FROM epp.epp_plan;
DELETE FROM receipts.receipt;
DELETE FROM payments.outbox_event;
DELETE FROM payments.payment_saga_step;
DELETE FROM payments.settlement_items;
DELETE FROM payments.settlement;
DELETE FROM payments.payment_allocation;
DELETE FROM payments.payment;
DELETE FROM payments.idempotency_key;
DELETE FROM notifications.notification;
DELETE FROM identity.moi_verification;
DELETE FROM billing.fee;
DELETE FROM integration.upload_error;
DELETE FROM integration.upload_batch;
DELETE FROM identity.guardian_student;
DELETE FROM identity.auth_account;
DELETE FROM identity.institution_user;
DELETE FROM billing.institution;
DELETE FROM identity.guardian;
DELETE FROM identity.student;
DELETE FROM identity.backoffice_employee
 WHERE id <> '22222222-2222-2222-2222-000000000000';
DELETE FROM common.audit_log;

-- ---------------------------------------------------------------------------
-- Backoffice: Nadia (admin) + Karim (bank officer)
-- ---------------------------------------------------------------------------
INSERT INTO identity.backoffice_employee (id, full_name, branch, role_id)
SELECT
    'a0a0a0a0-0001-4000-8000-000000000001',
    'Nadia Hassan',
    'Head Office',
    id
FROM identity.role
WHERE name = 'ADMIN';

INSERT INTO identity.backoffice_employee (id, full_name, branch, role_id)
SELECT
    'a0a0a0a0-0001-4000-8000-000000000002',
    'Karim El-Sayed',
    'Giza Main',
    id
FROM identity.role
WHERE name = 'BANK_OFFICER';

INSERT INTO identity.auth_account (
    id, username_normalized, password_hash, backoffice_employee_id, status
) VALUES
    (
        'f0f0f0f0-0006-4000-8000-000000000001',
        'nadia',
        '{bcrypt}$2y$10$9Wa0zTeEk3vzRc15waatg.GNzDfO7OoguyjF2/pkXsJKVeXA.fI5m',
        'a0a0a0a0-0001-4000-8000-000000000001',
        'ACTIVE'
    ),
    (
        'f0f0f0f0-0006-4000-8000-000000000002',
        'karim',
        '{bcrypt}$2y$10$9Wa0zTeEk3vzRc15waatg.GNzDfO7OoguyjF2/pkXsJKVeXA.fI5m',
        'a0a0a0a0-0001-4000-8000-000000000002',
        'ACTIVE'
    );

-- ---------------------------------------------------------------------------
-- Institutions
-- ---------------------------------------------------------------------------
INSERT INTO billing.institution (id, name, type, status, created_by) VALUES
    (
        'b0b0b0b0-0002-4000-8000-000000000001',
        'Nile Valley School',
        'SCHOOL',
        'ACTIVE',
        'a0a0a0a0-0001-4000-8000-000000000001'
    ),
    (
        'b0b0b0b0-0002-4000-8000-000000000002',
        'Maadi Language School',
        'SCHOOL',
        'ACTIVE',
        'a0a0a0a0-0001-4000-8000-000000000001'
    ),
    (
        'b0b0b0b0-0002-4000-8000-000000000003',
        'Closed Nile University',
        'UNIVERSITY',
        'INACTIVE',
        'a0a0a0a0-0001-4000-8000-000000000001'
    );

-- ---------------------------------------------------------------------------
-- Institution officers
-- ---------------------------------------------------------------------------
INSERT INTO identity.institution_user (id, institution_id, full_name, role_id)
SELECT
    'c0c0c0c0-0003-4000-8000-000000000001',
    'b0b0b0b0-0002-4000-8000-000000000001',
    'Laila Mostafa',
    id
FROM identity.role
WHERE name = 'INSTITUTION_OFFICER';

INSERT INTO identity.institution_user (id, institution_id, full_name, role_id)
SELECT
    'c0c0c0c0-0003-4000-8000-000000000002',
    'b0b0b0b0-0002-4000-8000-000000000002',
    'Heba Mostafa Zaki',
    id
FROM identity.role
WHERE name = 'INSTITUTION_OFFICER';

INSERT INTO identity.auth_account (
    id, username_normalized, password_hash, institution_user_id, status
) VALUES
    (
        'f0f0f0f0-0006-4000-8000-000000000003',
        'laila',
        '{bcrypt}$2y$10$9Wa0zTeEk3vzRc15waatg.GNzDfO7OoguyjF2/pkXsJKVeXA.fI5m',
        'c0c0c0c0-0003-4000-8000-000000000001',
        'ACTIVE'
    ),
    (
        'f0f0f0f0-0006-4000-8000-000000000004',
        'heba',
        '{bcrypt}$2y$10$9Wa0zTeEk3vzRc15waatg.GNzDfO7OoguyjF2/pkXsJKVeXA.fI5m',
        'c0c0c0c0-0003-4000-8000-000000000002',
        'ACTIVE'
    );

-- ---------------------------------------------------------------------------
-- Guardian, students, payer login (mock national IDs)
-- ---------------------------------------------------------------------------
INSERT INTO identity.guardian (id, national_id_hmac, national_id_encrypted, full_name, verified)
VALUES (
    'd0d0d0d0-0004-4000-8000-000000000001',
    '57c6d3e737694c9038d9488c6c5166cc676c39861cfde176093326c45e19f9fc',
    '\x00'::bytea,
    'Mona Samir Abdelrahman',
    TRUE
);

INSERT INTO identity.student (id, national_id_hmac, full_name, date_of_birth) VALUES
    (
        'e0e0e0e0-0005-4000-8000-000000000001',
        '2c785469c577cb14b27a0cbbf8afd754e466599540c5016da0afe00b151c5ce2',
        'Nour Khaled Fahmy',
        '2001-03-22'
    ),
    (
        'e0e0e0e0-0005-4000-8000-000000000002',
        'fbe9a0a6a6babcf8a2a1fd095c0a1b2e23c7e4442e75b66e998e1b80c761b95c',
        'Malak Hany Sobhy',
        '2012-04-01'
    );

INSERT INTO identity.guardian_student (guardian_id, student_id, relationship, source) VALUES
    (
        'd0d0d0d0-0004-4000-8000-000000000001',
        'e0e0e0e0-0005-4000-8000-000000000001',
        'MOTHER',
        'INSTITUTION_IMPORT'
    ),
    (
        'd0d0d0d0-0004-4000-8000-000000000001',
        'e0e0e0e0-0005-4000-8000-000000000002',
        'MOTHER',
        'INSTITUTION_IMPORT'
    );

INSERT INTO identity.auth_account (
    id, username_normalized, password_hash, guardian_id, status
) VALUES (
    'f0f0f0f0-0006-4000-8000-000000000005',
    'mona',
    '{bcrypt}$2y$10$9Wa0zTeEk3vzRc15waatg.GNzDfO7OoguyjF2/pkXsJKVeXA.fI5m',
    'd0d0d0d0-0004-4000-8000-000000000001',
    'ACTIVE'
);

-- ---------------------------------------------------------------------------
-- Upload history for GET /integration/uploads
-- ---------------------------------------------------------------------------
INSERT INTO integration.upload_batch (
    id, institution_id, uploaded_by, filename, uploaded_at, status,
    row_count, error_count, file_checksum, external_batch_reference
) VALUES
    (
        '10101010-0007-4000-8000-000000000001',
        'b0b0b0b0-0002-4000-8000-000000000001',
        'c0c0c0c0-0003-4000-8000-000000000001',
        'nile-valley-2026-09.csv',
        TIMESTAMPTZ '2026-09-10 09:00:00+03',
        'COMPLETED',
        4,
        0,
        'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
        'WALKTHROUGH-2026-09-COMPLETED'
    ),
    (
        '10101010-0007-4000-8000-000000000002',
        'b0b0b0b0-0002-4000-8000-000000000001',
        'c0c0c0c0-0003-4000-8000-000000000001',
        'nile-valley-2026-09-corrections.csv',
        TIMESTAMPTZ '2026-09-12 11:30:00+03',
        'COMPLETED_WITH_ERRORS',
        2,
        1,
        'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
        'WALKTHROUGH-2026-09-ERRORS'
    );

INSERT INTO integration.upload_error (id, batch_id, row_number, reason) VALUES
    (
        '10101010-0007-4000-8000-000000000011',
        '10101010-0007-4000-8000-000000000002',
        2,
        'Unknown fee type: SWIMMING'
    );

-- ---------------------------------------------------------------------------
-- Fees at Nile Valley School (enough outstanding left to collect live)
-- ---------------------------------------------------------------------------
INSERT INTO billing.fee
    (id, student_id, institution_id, fee_type, collection_period, currency,
     amount, amount_paid, status, source_batch_id)
VALUES
    (
        '20202020-0008-4000-8000-000000000001',
        'e0e0e0e0-0005-4000-8000-000000000001',
        'b0b0b0b0-0002-4000-8000-000000000001',
        'TUITION', '2026-09', 'EGP',
        18000.00, 8000.00, 'PARTIALLY_PAID',
        '10101010-0007-4000-8000-000000000001'
    ),
    (
        '20202020-0008-4000-8000-000000000002',
        'e0e0e0e0-0005-4000-8000-000000000001',
        'b0b0b0b0-0002-4000-8000-000000000001',
        'TRANSPORTATION', '2026-09', 'EGP',
        3500.00, 0.00, 'OUTSTANDING',
        '10101010-0007-4000-8000-000000000001'
    ),
    (
        '20202020-0008-4000-8000-000000000003',
        'e0e0e0e0-0005-4000-8000-000000000002',
        'b0b0b0b0-0002-4000-8000-000000000001',
        'TUITION', '2026-09', 'EGP',
        12000.00, 0.00, 'OUTSTANDING',
        '10101010-0007-4000-8000-000000000001'
    ),
    (
        '20202020-0008-4000-8000-000000000004',
        'e0e0e0e0-0005-4000-8000-000000000001',
        'b0b0b0b0-0002-4000-8000-000000000001',
        'OTHER', '2026-08', 'EGP',
        1500.00, 1500.00, 'SETTLED',
        '10101010-0007-4000-8000-000000000001'
    ),
    (
        '20202020-0008-4000-8000-000000000005',
        'e0e0e0e0-0005-4000-8000-000000000001',
        'b0b0b0b0-0002-4000-8000-000000000001',
        'ACTIVITIES', '2026-09', 'EGP',
        4000.00, 4000.00, 'SETTLED',
        '10101010-0007-4000-8000-000000000001'
    );

-- ---------------------------------------------------------------------------
-- Payments stamped with now() so institution/admin dashboards show "today"
-- ---------------------------------------------------------------------------
INSERT INTO payments.payment (
    id, idempotency_key, guardian_id, processed_by, method, payment_type,
    amount, currency, status, created_at, updated_at, settled_at,
    captured_amount, rrn, auth_code, response_code, response_message,
    masked_card_number, card_scheme, gateway_payment_id
) VALUES
    (
        '30303030-0009-4000-8000-000000000001',
        'walkthrough-cash-tuition-partial',
        'd0d0d0d0-0004-4000-8000-000000000001',
        'a0a0a0a0-0001-4000-8000-000000000002',
        'CASH', 'PARTIAL',
        8000.00, 'EGP', 'SUCCESSFUL',
        now() - interval '3 hours',
        now() - interval '3 hours',
        now() - interval '3 hours',
        8000.00, NULL, NULL, '00', 'Cash collected',
        NULL, NULL, NULL
    ),
    (
        '30303030-0009-4000-8000-000000000002',
        'walkthrough-cash-other-settled',
        'd0d0d0d0-0004-4000-8000-000000000001',
        'a0a0a0a0-0001-4000-8000-000000000002',
        'CASH', 'FULL',
        1500.00, 'EGP', 'SUCCESSFUL',
        now() - interval '2 hours',
        now() - interval '2 hours',
        now() - interval '2 hours',
        1500.00, NULL, NULL, '00', 'Cash collected',
        NULL, NULL, NULL
    ),
    (
        '30303030-0009-4000-8000-000000000003',
        'walkthrough-card-activities-epp',
        'd0d0d0d0-0004-4000-8000-000000000001',
        'a0a0a0a0-0001-4000-8000-000000000002',
        'CREDIT_CARD', 'EPP',
        4000.00, 'EGP', 'SUCCESSFUL',
        now() - interval '1 hour',
        now() - interval '1 hour',
        now() - interval '1 hour',
        4000.00, 'RRN-WALK-4000', 'AUTH4000', '00', 'Approved',
        '************4242', 'VISA', 'gw-pay-walkthrough-4000'
    );

INSERT INTO payments.payment_allocation (id, payment_id, fee_id, amount_applied) VALUES
    (
        '40404040-000a-4000-8000-000000000001',
        '30303030-0009-4000-8000-000000000001',
        '20202020-0008-4000-8000-000000000001',
        8000.00
    ),
    (
        '40404040-000a-4000-8000-000000000002',
        '30303030-0009-4000-8000-000000000002',
        '20202020-0008-4000-8000-000000000004',
        1500.00
    ),
    (
        '40404040-000a-4000-8000-000000000003',
        '30303030-0009-4000-8000-000000000003',
        '20202020-0008-4000-8000-000000000005',
        4000.00
    );

INSERT INTO receipts.receipt (id, payment_id, reference_number, generated_at) VALUES
    (
        '50505050-000b-4000-8000-000000000001',
        '30303030-0009-4000-8000-000000000001',
        'RCP-2026-WALK0001',
        now() - interval '3 hours'
    ),
    (
        '50505050-000b-4000-8000-000000000002',
        '30303030-0009-4000-8000-000000000002',
        'RCP-2026-WALK0002',
        now() - interval '2 hours'
    ),
    (
        '50505050-000b-4000-8000-000000000003',
        '30303030-0009-4000-8000-000000000003',
        'RCP-2026-WALK0003',
        now() - interval '1 hour'
    );

INSERT INTO payments.payment_saga_step (
    id, payment_id, step_name, status, attempted_at, completed_at
) VALUES
    (
        '60606060-000c-4000-8000-000000000001',
        '30303030-0009-4000-8000-000000000001',
        'SETTLE_FEES', 'COMPLETED',
        now() - interval '3 hours',
        now() - interval '3 hours'
    ),
    (
        '60606060-000c-4000-8000-000000000002',
        '30303030-0009-4000-8000-000000000001',
        'ISSUE_RECEIPT', 'COMPLETED',
        now() - interval '3 hours',
        now() - interval '3 hours'
    ),
    (
        '60606060-000c-4000-8000-000000000003',
        '30303030-0009-4000-8000-000000000003',
        'CHARGE', 'COMPLETED',
        now() - interval '1 hour',
        now() - interval '1 hour'
    );

INSERT INTO payments.outbox_event (
    id, aggregate_id, event_type, payload, status, created_at, published_at
) VALUES
    (
        '70707070-000d-4000-8000-000000000001',
        '30303030-0009-4000-8000-000000000001',
        'PAYMENT_SUCCEEDED',
        '{"paymentId":"30303030-0009-4000-8000-000000000001","amount":8000.00}',
        'PUBLISHED',
        now() - interval '3 hours',
        now() - interval '3 hours'
    ),
    (
        '70707070-000d-4000-8000-000000000003',
        '30303030-0009-4000-8000-000000000003',
        'PAYMENT_SUCCEEDED',
        '{"paymentId":"30303030-0009-4000-8000-000000000003","amount":4000.00}',
        'PUBLISHED',
        now() - interval '1 hour',
        now() - interval '1 hour'
    );

INSERT INTO epp.epp_plan (
    id, payment_id, tenor_months, principal, currency, annual_rate,
    interest_amount, admin_fee, monthly_instalment, status, gateway_plan_id
) VALUES (
    '80808080-000e-4000-8000-000000000001',
    '30303030-0009-4000-8000-000000000003',
    6,
    4000.00,
    'EGP',
    0.1800,
    240.00,
    50.00,
    715.00,
    'ACTIVE',
    'gw-epp-walkthrough-4000'
);

INSERT INTO epp.epp_instalment (id, plan_id, instalment_number, due_date, amount, status) VALUES
    ('80808080-000e-4000-8000-100000000001', '80808080-000e-4000-8000-000000000001', 1, '2026-10-16', 715.00, 'PAID'),
    ('80808080-000e-4000-8000-100000000002', '80808080-000e-4000-8000-000000000001', 2, '2026-11-16', 715.00, 'DUE'),
    ('80808080-000e-4000-8000-100000000003', '80808080-000e-4000-8000-000000000001', 3, '2026-12-16', 715.00, 'DUE'),
    ('80808080-000e-4000-8000-100000000004', '80808080-000e-4000-8000-000000000001', 4, '2027-01-16', 715.00, 'DUE'),
    ('80808080-000e-4000-8000-100000000005', '80808080-000e-4000-8000-000000000001', 5, '2027-02-16', 715.00, 'DUE'),
    ('80808080-000e-4000-8000-100000000006', '80808080-000e-4000-8000-000000000001', 6, '2027-03-16', 715.00, 'DUE');

INSERT INTO reporting.gateway_check_run (id, run_date, matched_count, unmatched_count) VALUES
    (
        '90909090-000f-4000-8000-000000000001',
        now(),
        1,
        0
    );

INSERT INTO reporting.gateway_check_item (id, report_id, payment_id, gateway_status, status) VALUES
    (
        '90909090-000f-4000-8000-100000000001',
        '90909090-000f-4000-8000-000000000001',
        '30303030-0009-4000-8000-000000000003',
        'CAPTURED',
        'MATCHED'
    );

COMMIT;
