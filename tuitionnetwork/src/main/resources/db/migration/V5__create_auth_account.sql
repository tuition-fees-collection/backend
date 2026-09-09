CREATE TABLE identity.auth_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    username_normalized VARCHAR(150) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    guardian_id UUID
        REFERENCES identity.guardian(id),

    backoffice_employee_id UUID
        REFERENCES identity.backoffice_employee(id),

    institution_user_id UUID
        REFERENCES identity.institution_user(id),

    status VARCHAR(30) NOT NULL,

    failed_login_attempts INTEGER
        NOT NULL DEFAULT 0,

    locked_until TIMESTAMPTZ,

    token_version BIGINT
        NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ
        NOT NULL DEFAULT now(),

    updated_at TIMESTAMPTZ
        NOT NULL DEFAULT now(),

    version BIGINT
        NOT NULL DEFAULT 0,

    CONSTRAINT ck_auth_account_one_profile
        CHECK (
            num_nonnulls(
                guardian_id,
                backoffice_employee_id,
                institution_user_id
            ) = 1
        ),

    CONSTRAINT ck_auth_account_status
        CHECK (
            status IN (
                'ACTIVE',
                'INACTIVE'
            )
        ),

    CONSTRAINT ck_auth_account_failed_attempts
        CHECK (
            failed_login_attempts >= 0
        ),

    CONSTRAINT ck_auth_account_token_version
        CHECK (
            token_version >= 0
        )
);