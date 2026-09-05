-- Schemas: one per module, per the module/package design
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS payments;
CREATE SCHEMA IF NOT EXISTS epp;
CREATE SCHEMA IF NOT EXISTS receipts;
CREATE SCHEMA IF NOT EXISTS notifications;
CREATE SCHEMA IF NOT EXISTS reporting;
CREATE SCHEMA IF NOT EXISTS integration;
CREATE SCHEMA IF NOT EXISTS common;

-- identity: roles, guardians, students, and the people who operate the system
CREATE TABLE identity.role (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE identity.guardian (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    national_id_hmac       VARCHAR(64) NOT NULL UNIQUE,
    national_id_encrypted  BYTEA NOT NULL,
    full_name              VARCHAR(255) NOT NULL,
    phone                  VARCHAR(50),
    email                  VARCHAR(255),
    has_cib_account        BOOLEAN NOT NULL DEFAULT FALSE,
    card_type              VARCHAR(20)
);

CREATE TABLE identity.student (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    national_id_hmac  VARCHAR(64) NOT NULL UNIQUE,
    full_name         VARCHAR(255) NOT NULL,
    date_of_birth     DATE
);

CREATE TABLE billing.institution (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(255) NOT NULL,
    type    VARCHAR(50) NOT NULL,
    status  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE identity.guardian_student (
    guardian_id   UUID NOT NULL REFERENCES identity.guardian(id),
    student_id    UUID NOT NULL REFERENCES identity.student(id),
    relationship  VARCHAR(50),
    PRIMARY KEY (guardian_id, student_id)
);

CREATE TABLE identity.backoffice_employee (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name  VARCHAR(255) NOT NULL,
    branch     VARCHAR(100),
    role_id    UUID NOT NULL REFERENCES identity.role(id)
);

CREATE TABLE identity.institution_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id  UUID NOT NULL REFERENCES billing.institution(id),
    full_name       VARCHAR(255) NOT NULL,
    role_id         UUID NOT NULL REFERENCES identity.role(id)
);

-- institution registered by an admin (a backoffice_employee with the ADMIN role)
ALTER TABLE billing.institution
    ADD COLUMN created_by UUID REFERENCES identity.backoffice_employee(id);

-- integration: getting dues in, from institutions with no system of their own or a real API
CREATE TABLE integration.upload_batch (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id  UUID NOT NULL REFERENCES billing.institution(id),
    uploaded_by     UUID NOT NULL REFERENCES identity.institution_user(id),
    filename        VARCHAR(255) NOT NULL,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    status          VARCHAR(20) NOT NULL,
    row_count       INTEGER NOT NULL DEFAULT 0,
    error_count     INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE integration.upload_error (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id    UUID NOT NULL REFERENCES integration.upload_batch(id),
    row_number  INTEGER NOT NULL,
    reason      TEXT NOT NULL
);

-- billing: the outstanding fee lines, the single source of truth for what is owed
CREATE TABLE billing.fee (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id         UUID NOT NULL REFERENCES identity.student(id),
    institution_id     UUID NOT NULL REFERENCES billing.institution(id),
    fee_type           VARCHAR(50) NOT NULL,
    collection_period  VARCHAR(50) NOT NULL,
    currency           VARCHAR(3) NOT NULL DEFAULT 'EGP',
    amount             NUMERIC(12,2) NOT NULL,
    amount_paid        NUMERIC(12,2) NOT NULL DEFAULT 0,
    status             VARCHAR(20) NOT NULL DEFAULT 'OUTSTANDING',
    source_batch_id    UUID REFERENCES integration.upload_batch(id),
    version            INTEGER NOT NULL DEFAULT 0
);

-- payments: every movement of money, and the saga that keeps it consistent
CREATE TABLE payments.idempotency_key (
    key_value          VARCHAR(255) PRIMARY KEY,
    endpoint           VARCHAR(255) NOT NULL,
    request_hash       VARCHAR(255) NOT NULL,
    response_snapshot  TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payments.payment (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key  VARCHAR(255) NOT NULL UNIQUE,
    guardian_id      UUID NOT NULL REFERENCES identity.guardian(id),
    processed_by     UUID NOT NULL REFERENCES identity.backoffice_employee(id),
    method           VARCHAR(20) NOT NULL,
    payment_type     VARCHAR(20) NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    settled_at       TIMESTAMPTZ,
    version          INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE payments.payment_allocation (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id       UUID NOT NULL REFERENCES payments.payment(id),
    fee_id           UUID NOT NULL REFERENCES billing.fee(id),
    amount_applied   NUMERIC(12,2) NOT NULL
);

CREATE TABLE payments.payment_saga_step (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id      UUID NOT NULL REFERENCES payments.payment(id),
    step_name       VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempted_at    TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error_message   TEXT
);

CREATE TABLE payments.outbox_event (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id  UUID NOT NULL REFERENCES payments.payment(id),
    event_type    VARCHAR(50) NOT NULL,
    payload       TEXT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ
);

CREATE TABLE payments.settlement (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id  UUID NOT NULL REFERENCES billing.institution(id),
    amount          NUMERIC(12,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settled_at      TIMESTAMPTZ
);

CREATE TABLE payments.settlement_items (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id  UUID NOT NULL REFERENCES payments.settlement(id),
    payment_id     UUID NOT NULL REFERENCES payments.payment(id)
);

-- epp: instalment eligibility, pricing, and the schedule
CREATE TABLE epp.epp_plan (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id          UUID NOT NULL REFERENCES payments.payment(id),
    tenor_months        INTEGER NOT NULL,
    principal           NUMERIC(12,2) NOT NULL,
    interest            NUMERIC(12,2) NOT NULL,
    admin_fee           NUMERIC(12,2) NOT NULL,
    monthly_instalment  NUMERIC(12,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE epp.epp_instalment (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id             UUID NOT NULL REFERENCES epp.epp_plan(id),
    instalment_number   INTEGER NOT NULL,
    due_date            DATE NOT NULL,
    amount              NUMERIC(12,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

-- receipts: the document that proves a payment happened
CREATE TABLE receipts.receipt (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id        UUID NOT NULL REFERENCES payments.payment(id),
    reference_number  VARCHAR(50) NOT NULL UNIQUE,
    generated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- notifications: reminders, deadlines and confirmations
CREATE TABLE notifications.notification (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guardian_id  UUID NOT NULL REFERENCES identity.guardian(id),
    payment_id   UUID REFERENCES payments.payment(id),
    type         VARCHAR(30) NOT NULL,
    channel      VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at      TIMESTAMPTZ
);

-- reporting: daily summaries and reconciliation against the bank's core ledger
CREATE TABLE reporting.daily_reports (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id     UUID NOT NULL REFERENCES billing.institution(id),
    report_date        DATE NOT NULL,
    settled_total      NUMERIC(14,2) NOT NULL DEFAULT 0,
    partial_total      NUMERIC(14,2) NOT NULL DEFAULT 0,
    outstanding_total  NUMERIC(14,2) NOT NULL DEFAULT 0,
    UNIQUE (institution_id, report_date)
);

CREATE TABLE reporting.reconciliation_reports (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_date         TIMESTAMPTZ NOT NULL DEFAULT now(),
    matched_count    INTEGER NOT NULL DEFAULT 0,
    unmatched_count  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE reporting.reconciliation_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id        UUID NOT NULL REFERENCES reporting.reconciliation_reports(id),
    payment_id       UUID NOT NULL REFERENCES payments.payment(id),
    bank_ledger_ref  VARCHAR(255),
    status           VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED'
);

-- common: cross-cutting audit trail, referencing whichever actor type performed the action
CREATE TABLE common.audit_log (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_type                VARCHAR(30) NOT NULL,
    actor_id                  UUID NOT NULL,
    action                    VARCHAR(100) NOT NULL,
    target_national_id_hmac   VARCHAR(64),
    occurred_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);
