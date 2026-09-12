ALTER TABLE common.audit_log
    ALTER COLUMN actor_type DROP NOT NULL,
    ALTER COLUMN actor_id DROP NOT NULL;

ALTER TABLE common.audit_log
    ADD CONSTRAINT ck_audit_log_actor_pair CHECK (
        (actor_type IS NULL AND actor_id IS NULL)
        OR (actor_type IS NOT NULL AND actor_id IS NOT NULL)
    );
