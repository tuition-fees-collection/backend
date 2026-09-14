package com.tuitionnetwork.tuitionnetwork.integration.web;

import com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


public record UploadBatchView(
        UUID id,
        UUID institutionId,
        UUID uploadedBy,
        String filename,
        String fileChecksum,
        String externalBatchReference,
        OffsetDateTime uploadedAt,
        String status,
        int rowCount,
        int errorCount,
        List<RowErrorView> errors) {

    public record RowErrorView(int rowNumber, String reason) {

        static RowErrorView from(com.tuitionnetwork.tuitionnetwork.integration.domain.UploadError error) {
            return new RowErrorView(error.getRowNumber(), error.getReason());
        }
    }

    public static UploadBatchView from(UploadBatch batch) {
        return new UploadBatchView(
                batch.getId(),
                batch.getInstitutionId(),
                batch.getUploadedBy(),
                batch.getFilename(),
                batch.getFileChecksum(),
                batch.getExternalBatchReference(),
                batch.getUploadedAt(),
                batch.getStatus().name(),
                batch.getRowCount(),
                batch.getErrorCount(),
                batch.getErrors().stream().map(RowErrorView::from).toList());
    }
}
