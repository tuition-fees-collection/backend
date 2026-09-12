ALTER TABLE integration.upload_batch
    ADD COLUMN file_checksum VARCHAR(64),
    ADD COLUMN external_batch_reference VARCHAR(255);

ALTER TABLE integration.upload_batch
    ALTER COLUMN status TYPE VARCHAR(30);

UPDATE integration.upload_batch
SET file_checksum = repeat('0', 32) || replace(id::text, '-', '')
WHERE file_checksum IS NULL;

ALTER TABLE integration.upload_batch
    ALTER COLUMN file_checksum SET NOT NULL,
    ADD CONSTRAINT ck_upload_batch_checksum_format
        CHECK (file_checksum ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT uq_upload_batch_institution_checksum
        UNIQUE (institution_id, file_checksum),
    ADD CONSTRAINT uq_upload_batch_institution_external_ref
        UNIQUE (institution_id, external_batch_reference);
