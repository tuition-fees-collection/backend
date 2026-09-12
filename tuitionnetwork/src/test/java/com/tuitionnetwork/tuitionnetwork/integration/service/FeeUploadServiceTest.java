package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.infra.UploadBatchRepository;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.FeeProvisioningPort;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.GuardianProvisioningPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeeUploadServiceTest {

    private final FeeUploadValidator validator = mock(FeeUploadValidator.class);
    private final UploadBatchRepository uploadBatchRepository = mock(UploadBatchRepository.class);
    private final GuardianProvisioningPort guardianProvisioningPort = mock(GuardianProvisioningPort.class);
    private final FeeProvisioningPort feeProvisioningPort = mock(FeeProvisioningPort.class);

    private InputStream anyCsv() {
        return new ByteArrayInputStream(new byte[0]);
    }

    private FeeUploadRow acceptedRow(String parentNationalId, String parentFullName) {
        return new FeeUploadRow(1, "29901234567890", "TUITION", BigDecimal.valueOf(5000), "EGP",
                "2026-10", parentNationalId, parentFullName);
    }

    private FeeUploadValidator.ValidationOutcome validOutcome(FeeUploadRow row) {
        return new FeeUploadValidator.ValidationOutcome(List.of(row), List.of(), 1, "a".repeat(64));
    }

    @Test
    void callsGuardianProvisioningPortForEachAcceptedRowWhenPresent() {
        FeeUploadRow row = acceptedRow("19001112223344", "Ahmed Mohamed");
        when(validator.validate(any())).thenReturn(validOutcome(row));
        when(uploadBatchRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(guardianProvisioningPort.upsertGuardianStub(any(), any())).thenReturn(UUID.randomUUID());

        FeeUploadService service = new FeeUploadService(validator, uploadBatchRepository,
                Optional.of(guardianProvisioningPort), Optional.empty());

        service.submit(UUID.randomUUID(), UUID.randomUUID(), "dues.csv", anyCsv());

        verify(guardianProvisioningPort).upsertGuardianStub("19001112223344", "Ahmed Mohamed");
    }

    @Test
    void skipsGuardianProvisioningSilentlyWhenPortIsAbsent() {
        FeeUploadRow row = acceptedRow("19001112223344", "Ahmed Mohamed");
        when(validator.validate(any())).thenReturn(validOutcome(row));
        when(uploadBatchRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FeeUploadService service = new FeeUploadService(
                validator, uploadBatchRepository, Optional.empty(), Optional.empty());

        FeeUploadResult result = service.submit(UUID.randomUUID(), UUID.randomUUID(), "dues.csv", anyCsv());

        verify(guardianProvisioningPort, never()).upsertGuardianStub(any(), any());
        assertThat(result.acceptedRows()).hasSize(1);
    }

    @Test
    void rejectsAnExistingFileChecksumBeforeCreatingAnotherBatch() {
        FeeUploadRow row = acceptedRow("19001112223344", "Ahmed Mohamed");
        var existing = com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch.start(
                UUID.randomUUID(), UUID.randomUUID(), "first.csv", "a".repeat(64), "BATCH-1");
        existing.finish(1, 1);
        when(validator.validate(any())).thenReturn(validOutcome(row));
        when(uploadBatchRepository.findFirstByInstitutionIdAndFileChecksum(any(), any()))
                .thenReturn(Optional.of(existing));

        FeeUploadService service = new FeeUploadService(
                validator, uploadBatchRepository, Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> service.submit(
                UUID.randomUUID(), UUID.randomUUID(), "second.csv", "BATCH-2", anyCsv()))
                .isInstanceOf(DuplicateUploadException.class)
                .satisfies(error -> assertThat(((DuplicateUploadException) error).matchedBy())
                        .isEqualTo("FILE_CHECKSUM"));

        verify(uploadBatchRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAnExistingExternalBatchReferenceBeforeCheckingContent() {
        FeeUploadRow row = acceptedRow("19001112223344", "Ahmed Mohamed");
        var existing = com.tuitionnetwork.tuitionnetwork.integration.domain.UploadBatch.start(
                UUID.randomUUID(), UUID.randomUUID(), "first.csv", "b".repeat(64), "BATCH-1");
        existing.finish(1, 1);
        when(validator.validate(any())).thenReturn(validOutcome(row));
        when(uploadBatchRepository.findFirstByInstitutionIdAndExternalBatchReference(any(), any()))
                .thenReturn(Optional.of(existing));

        FeeUploadService service = new FeeUploadService(
                validator, uploadBatchRepository, Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> service.submit(
                UUID.randomUUID(), UUID.randomUUID(), "changed.csv", "BATCH-1", anyCsv()))
                .isInstanceOf(DuplicateUploadException.class)
                .satisfies(error -> assertThat(((DuplicateUploadException) error).matchedBy())
                        .isEqualTo("EXTERNAL_BATCH_REFERENCE"));

        verify(uploadBatchRepository, never()).saveAndFlush(any());
    }

    @Test
    void mapsConcurrentDatabaseUniqueViolationToDuplicateUpload() {
        FeeUploadRow row = acceptedRow("19001112223344", "Ahmed Mohamed");
        when(validator.validate(any())).thenReturn(validOutcome(row));
        SQLException uniqueViolation = new SQLException(
                "uq_upload_batch_institution_checksum", "23505");
        when(uploadBatchRepository.saveAndFlush(any())).thenThrow(
                new DataIntegrityViolationException("duplicate", uniqueViolation));

        FeeUploadService service = new FeeUploadService(
                validator, uploadBatchRepository, Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> service.submit(
                UUID.randomUUID(), UUID.randomUUID(), "dues.csv", null, anyCsv()))
                .isInstanceOf(DuplicateUploadException.class)
                .satisfies(error -> assertThat(((DuplicateUploadException) error).matchedBy())
                        .isEqualTo("FILE_CHECKSUM"));
    }

    @Test
    void provisionsBillingFeeForEveryAcceptedCsvRow() {
        FeeUploadRow row = acceptedRow("19001112223344", "Ahmed Mohamed");
        UUID institutionId = UUID.randomUUID();
        when(validator.validate(any())).thenReturn(validOutcome(row));
        when(feeProvisioningPort.isAvailable()).thenReturn(true);
        when(uploadBatchRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object batch = invocation.getArgument(0);
            ReflectionTestUtils.setField(batch, "id", UUID.randomUUID());
            return batch;
        });

        FeeUploadService service = new FeeUploadService(
                validator,
                uploadBatchRepository,
                Optional.empty(),
                Optional.of(feeProvisioningPort));

        service.submit(institutionId, UUID.randomUUID(), "dues.csv", anyCsv());

        ArgumentCaptor<FeeProvisioningPort.Command> command =
                ArgumentCaptor.forClass(FeeProvisioningPort.Command.class);
        verify(feeProvisioningPort).provisionFee(command.capture());
        assertThat(command.getValue().studentNationalId()).isEqualTo("29901234567890");
        assertThat(command.getValue().institutionId()).isEqualTo(institutionId);
        assertThat(command.getValue().feeType()).isEqualTo("TUITION");
        assertThat(command.getValue().amount()).isEqualByComparingTo("5000");
        assertThat(command.getValue().sourceBatchId()).isNotNull();
    }
}
