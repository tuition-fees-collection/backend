package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch;
import com.tuitionnetwork.tuitionnetwork.integration.infra.UploadBatchRepository;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.GuardianProvisioningPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FeeUploadService {

    private final FeeUploadValidator validator;
    private final UploadBatchRepository uploadBatchRepository;


    private final Optional<GuardianProvisioningPort> guardianProvisioningPort;

    public FeeUploadService(FeeUploadValidator validator, UploadBatchRepository uploadBatchRepository,
                             Optional<GuardianProvisioningPort> guardianProvisioningPort) {
        this.validator = validator;
        this.uploadBatchRepository = uploadBatchRepository;
        this.guardianProvisioningPort = guardianProvisioningPort;
    }

    @Transactional
    public FeeUploadResult submit(UUID institutionId, UUID uploadedBy, String filename, InputStream csv) {
        return submit(institutionId, uploadedBy, filename, null, csv);
    }

    @Transactional
    public FeeUploadResult submit(UUID institutionId, UUID uploadedBy, String filename,
                                  String externalBatchReference, InputStream csv) {
        String normalizedReference = normalizeExternalReference(externalBatchReference);

        FeeUploadValidator.ValidationOutcome outcome = validator.validate(csv);
        rejectExistingUpload(institutionId, normalizedReference, outcome.fileChecksum());

        UploadBatch batch = UploadBatch.start(
                institutionId, uploadedBy, filename, outcome.fileChecksum(), normalizedReference);
        outcome.errors().forEach(error -> batch.addError(error.rowNumber(), error.reason()));
        batch.finish(outcome.totalRows(), outcome.acceptedRows().size());

        try {
            uploadBatchRepository.saveAndFlush(batch);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                throw DuplicateUploadException.fromConcurrentSubmission(matchedConstraint(e));
            }
            throw e;
        }

        guardianProvisioningPort.ifPresent(port -> {
            for (FeeUploadRow row : outcome.acceptedRows()) {
                port.upsertGuardianStub(row.parentNationalId(), row.parentFullName());

            }
        });

        return new FeeUploadResult(
                batch.getId(),
                batch.getStatus().name(),
                batch.getFileChecksum(),
                batch.getExternalBatchReference(),
                batch.getRowCount(),
                batch.getErrorCount(),
                outcome.acceptedRows(),
                List.copyOf(outcome.errors()));
    }

    private void rejectExistingUpload(UUID institutionId, String externalReference, String checksum) {
        if (externalReference != null) {
            uploadBatchRepository
                    .findFirstByInstitutionIdAndExternalBatchReference(institutionId, externalReference)
                    .ifPresent(batch -> {
                        throw DuplicateUploadException.forExisting(batch, "EXTERNAL_BATCH_REFERENCE");
                    });
        }

        uploadBatchRepository.findFirstByInstitutionIdAndFileChecksum(institutionId, checksum)
                .ifPresent(batch -> {
                    throw DuplicateUploadException.forExisting(batch, "FILE_CHECKSUM");
                });
    }

    private String normalizeExternalReference(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        String normalized = reference.trim();
        if (normalized.length() > 255) {
            throw new UploadValidationException(
                    "EXTERNAL_REFERENCE_TOO_LONG", "External batch reference cannot exceed 255 characters");
        }
        return normalized;
    }

    private boolean isUniqueViolation(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String matchedConstraint(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("uq_upload_batch_institution_external_ref")) {
                return "EXTERNAL_BATCH_REFERENCE";
            }
            if (message != null && message.contains("uq_upload_batch_institution_checksum")) {
                return "FILE_CHECKSUM";
            }
            current = current.getCause();
        }
        return "DATABASE_UNIQUE_CONSTRAINT";
    }
}
