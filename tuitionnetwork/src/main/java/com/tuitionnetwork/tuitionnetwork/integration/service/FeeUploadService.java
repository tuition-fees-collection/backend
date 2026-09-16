package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch;
import com.tuitionnetwork.tuitionnetwork.integration.infra.UploadBatchRepository;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.FeeProvisioningPort;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.GuardianProvisioningPort;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.StudentResolutionPort;
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
    private final Optional<StudentResolutionPort> studentResolutionPort;
    private final Optional<FeeProvisioningPort> feeProvisioningPort;

    public FeeUploadService(FeeUploadValidator validator, UploadBatchRepository uploadBatchRepository,
                            Optional<GuardianProvisioningPort> guardianProvisioningPort,
                            Optional<StudentResolutionPort> studentResolutionPort,
                            Optional<FeeProvisioningPort> feeProvisioningPort) {
        this.validator = validator;
        this.uploadBatchRepository = uploadBatchRepository;
        this.guardianProvisioningPort = guardianProvisioningPort;
        this.studentResolutionPort = studentResolutionPort;
        this.feeProvisioningPort = feeProvisioningPort;
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
                UUID guardianId = port.upsertGuardianStub(
                        row.parentNationalId(), row.parentFullName());
                studentResolutionPort.filter(StudentResolutionPort::isAvailable).ifPresent(resolver -> {
                    UUID studentId = resolver.resolveRequiredStudentId(row.nationalId());
                    port.linkGuardianToStudent(guardianId, studentId);
                });
            }
        });

        feeProvisioningPort.filter(FeeProvisioningPort::isAvailable).ifPresent(port -> {
            for (FeeUploadRow row : outcome.acceptedRows()) {
                port.provisionFee(new FeeProvisioningPort.Command(
                        row.nationalId(),
                        institutionId,
                        row.feeType(),
                        row.collectionPeriod(),
                        row.currency(),
                        row.amount(),
                        batch.getId()));
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

    public UploadBatch getBatch(UUID batchId) {
        return uploadBatchRepository.findByIdWithErrors(batchId)
                .orElseThrow(() -> new UploadBatchNotFoundException(batchId));
    }

    public List<UploadBatch> findByInstitution(UUID institutionId) {
        return uploadBatchRepository.findByInstitutionIdWithErrorsOrderByUploadedAtDesc(institutionId);
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
