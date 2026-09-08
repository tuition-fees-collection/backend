package com.tuitionnetwork.tuitionnetwork.integration.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "upload_batch", schema = "integration")
public class UploadBatch {

    @Id
    @GeneratedValue
    private UUID id;


    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(nullable = false)
    private String filename;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus status;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rowNumber asc")
    private List<UploadError> errors = new ArrayList<>();

    protected UploadBatch() {
        
    }

    public static UploadBatch start(UUID institutionId, UUID uploadedBy, String filename) {
        UploadBatch batch = new UploadBatch();
        batch.institutionId = institutionId;
        batch.uploadedBy = uploadedBy;
        batch.filename = filename;
        batch.uploadedAt = OffsetDateTime.now();
        batch.status = UploadStatus.PROCESSING;
        return batch;
    }

    public void addError(int rowNumber, String reason) {
        errors.add(new UploadError(this, rowNumber, reason));
    }

    public void finish(int totalRows) {
        this.rowCount = totalRows;
        this.errorCount = errors.size();
        this.status = errorCount == 0
                ? UploadStatus.COMPLETED
                : (errorCount == totalRows ? UploadStatus.FAILED : UploadStatus.COMPLETED_WITH_ERRORS);
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public String getFilename() {
        return filename;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<UploadError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
