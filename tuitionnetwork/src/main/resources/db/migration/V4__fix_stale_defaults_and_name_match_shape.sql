
ALTER TABLE epp.epp_instalment ALTER COLUMN status SET DEFAULT 'DUE';


ALTER TABLE identity.moi_verification RENAME COLUMN name_matched TO name_match_matched;
ALTER TABLE identity.moi_verification
    ADD COLUMN name_match_provided  VARCHAR(255),
    ADD COLUMN name_match_score     NUMERIC(5,4),
    ADD COLUMN name_match_note      TEXT;


ALTER TABLE identity.guardian
    DROP COLUMN phone,
    DROP COLUMN email,
    DROP COLUMN has_cib_account,
    DROP COLUMN card_type;


ALTER TABLE identity.guardian
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE identity.guardian_student
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'INSTITUTION_IMPORT';
