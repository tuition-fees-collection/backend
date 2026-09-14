package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch;

import java.util.UUID;

public class DuplicateUploadException extends RuntimeException {

    private final UUID existingBatchId;
    private final String existingStatus;
    private final String matchedBy;

    private DuplicateUploadException(UUID existingBatchId, String existingStatus, String matchedBy) {
        super("This upload was already submitted for the institution");
        this.existingBatchId = existingBatchId;
        this.existingStatus = existingStatus;
        this.matchedBy = matchedBy;
    }

    public static DuplicateUploadException forExisting(UploadBatch batch, String matchedBy) {
        return new DuplicateUploadException(batch.getId(), batch.getStatus().name(), matchedBy);
    }

    public static DuplicateUploadException fromConcurrentSubmission(String matchedBy) {
        return new DuplicateUploadException(null, null, matchedBy);
    }

    public UUID existingBatchId() {
        return existingBatchId;
    }

    public String existingStatus() {
        return existingStatus;
    }

    public String matchedBy() {
        return matchedBy;
    }
}
