UPDATE billing.institution
SET status = 'ACTIVE'
WHERE status IS NULL OR status NOT IN ('ACTIVE', 'INACTIVE');

ALTER TABLE billing.institution
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE billing.institution
    ADD CONSTRAINT ck_institution_status
    CHECK (status IN ('ACTIVE', 'INACTIVE'));
