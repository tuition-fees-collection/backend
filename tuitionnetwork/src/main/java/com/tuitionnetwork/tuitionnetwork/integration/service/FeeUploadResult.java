package com.tuitionnetwork.tuitionnetwork.integration.service;

import java.util.List;
import java.util.UUID;

public record FeeUploadResult(
        UUID batchId,
        String status,
        int rowCount,
        int errorCount,
        List<FeeUploadRow> acceptedRows,
        List<RowError> errors) {

    public record RowError(int rowNumber, String reason) {
    }
}
