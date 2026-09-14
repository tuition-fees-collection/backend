INSERT INTO identity.role (
    id,
    name,
    description
)
VALUES
    (
        gen_random_uuid(),
        'PAYER',
        'Guardian or payer'
    ),
    (
        gen_random_uuid(),
        'BANK_OFFICER',
        'Bank back-office employee'
    ),
    (
        gen_random_uuid(),
        'INSTITUTION_OFFICER',
        'Institution user'
    ),
    (
        gen_random_uuid(),
        'ADMIN',
        'Identity and administrative user'
    )
ON CONFLICT (name) DO NOTHING;