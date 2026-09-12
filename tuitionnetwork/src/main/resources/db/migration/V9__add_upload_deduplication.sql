ALTER TABLE integration.upload_batch
    ADD COLUMN file_checksum VARCHAR(64),
    ADD COLUMN external_batch_reference VARCHAR(255);

-- COMPLETED_WITH_ERRORS is 21 characters; the original VARCHAR(20) only
-- appeared to work while inserts were not flushed inside the service.
ALTER TABLE integration.upload_batch
    ALTER COLUMN status TYPE VARCHAR(30);

-- Existing batches predate content hashing. Give each one a deterministic,
-- unique legacy value without pretending it is a checksum of the old file.
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
