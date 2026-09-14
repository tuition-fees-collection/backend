package com.tuitionnetwork.tuitionnetwork.integration.web;

import java.util.UUID;

public record DuplicateUploadResponse(
        String code,
        String message,
        UUID existingBatchId,
        String existingStatus,
        String matchedBy) {
}
