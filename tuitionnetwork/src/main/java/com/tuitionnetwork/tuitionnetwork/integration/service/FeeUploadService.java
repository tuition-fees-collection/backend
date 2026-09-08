package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch;
import com.tuitionnetwork.tuitionnetwork.integration.infra.UploadBatchRepository;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.GuardianProvisioningPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
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
        UploadBatch batch = UploadBatch.start(institutionId, uploadedBy, filename);

        FeeUploadValidator.ValidationOutcome outcome = validator.validate(csv);
        outcome.errors().forEach(error -> batch.addError(error.rowNumber(), error.reason()));
        batch.finish(outcome.totalRows());

        uploadBatchRepository.save(batch);

        guardianProvisioningPort.ifPresent(port -> {
            for (FeeUploadRow row : outcome.acceptedRows()) {
                port.upsertGuardianStub(row.parentNationalId(), row.parentFullName());

            }
        });

        return new FeeUploadResult(
                batch.getId(),
                batch.getStatus().name(),
                batch.getRowCount(),
                batch.getErrorCount(),
                outcome.acceptedRows(),
                List.copyOf(outcome.errors()));
    }
}
