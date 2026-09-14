package com.tuitionnetwork.tuitionnetwork.integration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "upload_error", schema = "integration")
public class UploadError {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private UploadBatch batch;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(nullable = false)
    private String reason;

    protected UploadError() {
       
    }

    UploadError(UploadBatch batch, Integer rowNumber, String reason) {
        this.batch = batch;
        this.rowNumber = rowNumber;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public String getReason() {
        return reason;
    }
}
