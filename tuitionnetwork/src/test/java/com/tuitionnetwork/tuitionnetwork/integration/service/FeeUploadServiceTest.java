package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.infra.UploadBatchRepository;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.GuardianProvisioningPort;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeeUploadServiceTest {

    private final FeeUploadValidator validator = mock(FeeUploadValidator.class);
    private final UploadBatchRepository uploadBatchRepository = mock(UploadBatchRepository.class);
    private final GuardianProvisioningPort guardianProvisioningPort = mock(GuardianProvisioningPort.class);

    private InputStream anyCsv() {
        return new ByteArrayInputStream(new byte[0]);
    }

    private FeeUploadRow acceptedRow(String parentNationalId, String parentFullName) {
        return new FeeUploadRow(1, "29901234567890", "TUITION", BigDecimal.valueOf(5000), "EGP",
                "2026-10", parentNationalId, parentFullName);
    }

    @Test
    void callsGuardianProvisioningPortForEachAcceptedRowWhenPresent() {
        FeeUploadRow row = acceptedRow("19001112223344", "Ahmed Mohamed");
        when(validator.validate(any())).thenReturn(
                new FeeUploadValidator.ValidationOutcome(List.of(row), List.of(), 1));
        when(uploadBatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(guardianProvisioningPort.upsertGuardianStub(any(), any())).thenReturn(UUID.randomUUID());

        FeeUploadService service = new FeeUploadService(validator, uploadBatchRepository,
                Optional.of(guardianProvisioningPort));

        service.submit(UUID.randomUUID(), UUID.randomUUID(), "dues.csv", anyCsv());

        verify(guardianProvisioningPort).upsertGuardianStub("19001112223344", "Ahmed Mohamed");
    }

    @Test
    void skipsGuardianProvisioningSilentlyWhenPortIsAbsent() {
        FeeUploadRow row = acceptedRow("19001112223344", "Ahmed Mohamed");
        when(validator.validate(any())).thenReturn(
                new FeeUploadValidator.ValidationOutcome(List.of(row), List.of(), 1));
        when(uploadBatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FeeUploadService service = new FeeUploadService(validator, uploadBatchRepository, Optional.empty());

        FeeUploadResult result = service.submit(UUID.randomUUID(), UUID.randomUUID(), "dues.csv", anyCsv());

        verify(guardianProvisioningPort, never()).upsertGuardianStub(any(), any());
        assertThat(result.acceptedRows()).hasSize(1);
    }
}
