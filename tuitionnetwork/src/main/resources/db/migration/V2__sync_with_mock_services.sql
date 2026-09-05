-- Rename the reconciliation tables to reflect what they actually check
-- (the gateway's own record, not an independent bank ledger — see design notes)
ALTER TABLE reporting.reconciliation_reports RENAME TO gateway_check_run;
ALTER TABLE reporting.reconciliation_items RENAME TO gateway_check_item;
ALTER TABLE reporting.gateway_check_item RENAME COLUMN bank_ledger_ref TO gateway_status;

-- payments.payment: fields learned from the real mock service contract
ALTER TABLE payments.payment
    ADD COLUMN gateway_payment_id  VARCHAR(255),
    ADD COLUMN currency            VARCHAR(3) NOT NULL DEFAULT 'EGP',
    ADD COLUMN auth_code           VARCHAR(50),
    ADD COLUMN rrn                 VARCHAR(50),
    ADD COLUMN response_code       VARCHAR(10),
    ADD COLUMN response_message    VARCHAR(255),
    ADD COLUMN masked_card_number  VARCHAR(30),
    ADD COLUMN card_scheme         VARCHAR(20),
    ADD COLUMN captured_amount     NUMERIC(12,2),
    ADD COLUMN refunded_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN updated_at          TIMESTAMPTZ;

UPDATE payments.payment SET updated_at = created_at WHERE updated_at IS NULL;

-- epp.epp_plan: same pass, plus the mock's own field name for the interest amount
ALTER TABLE epp.epp_plan
    ADD COLUMN gateway_plan_id VARCHAR(255),
    ADD COLUMN currency       VARCHAR(3) NOT NULL DEFAULT 'EGP',
    ADD COLUMN annual_rate    NUMERIC(6,4);

ALTER TABLE epp.epp_plan RENAME COLUMN interest TO interest_amount;

-- identity: national ID verification history against the MOI mock
CREATE TABLE identity.moi_verification (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guardian_id       UUID NOT NULL REFERENCES identity.guardian(id),
    verification_id   VARCHAR(255) NOT NULL,
    purpose           VARCHAR(30) NOT NULL,
    valid             BOOLEAN NOT NULL,
    record_status     VARCHAR(20) NOT NULL,
    reasons           TEXT,
    birth_date        DATE,
    gender            VARCHAR(10),
    governorate       VARCHAR(50),
    governorate_code  VARCHAR(5),
    checksum_valid    BOOLEAN,
    name_matched      BOOLEAN,
    verified_at       TIMESTAMPTZ NOT NULL
);
