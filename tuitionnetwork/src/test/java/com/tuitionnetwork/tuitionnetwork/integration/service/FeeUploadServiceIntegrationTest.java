package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.infra.UploadBatchRepository;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.FeeProvisioningPort;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.StudentResolutionPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@ActiveProfiles("test")
@SpringBootTest
@Transactional
class FeeUploadServiceIntegrationTest {

    @Autowired
    private FeeUploadService feeUploadService;

    @Autowired
    private UploadBatchRepository uploadBatchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private FeeProvisioningPort feeProvisioningPort;

    @MockitoBean
    private StudentResolutionPort studentResolutionPort;

    private final String futurePeriod = YearMonth.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

    private UUID seedInstitution() {
        UUID institutionId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO billing.institution (id, name, type, status) VALUES (?, ?, ?, ?)",
                institutionId, "Test Institution", "SCHOOL", "ACTIVE");
        return institutionId;
    }

    private UUID seedInstitutionUser(UUID institutionId) {
        UUID roleId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity.role (id, name, description) VALUES (?, ?, ?)",
                roleId, "UPLOADER_" + roleId.toString().substring(0, 8), "Seeded for FeeUploadServiceIntegrationTest");

        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity.institution_user (id, institution_id, full_name, role_id) VALUES (?, ?, ?, ?)",
                userId, institutionId, "Test Uploader", roleId);
        return userId;
    }

    private InputStream csv(String... lines) {
        String content = "national_id,fee_type,amount,currency,collection_period,parent_national_id,parent_full_name\n"
                + String.join("\n", lines);
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void submittingAMixedFilePersistsBatchAndRowErrors() {
        UUID institutionId = seedInstitution();
        UUID uploadedBy = seedInstitutionUser(institutionId);

        InputStream file = csv(
                "29901234567890,TUITION,5000,EGP," + futurePeriod + ",19001112223344,Ahmed Mohamed",
                "30001234567890,SWIMMING,500,EGP," + futurePeriod + ",19001112223344,Ahmed Mohamed");

        FeeUploadResult result = feeUploadService.submit(institutionId, uploadedBy, "dues.csv", file);

        assertThat(result.status()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(result.rowCount()).isEqualTo(2);
        assertThat(result.errorCount()).isEqualTo(1);
        assertThat(result.acceptedRows()).hasSize(1);
        assertThat(result.errors()).hasSize(1);

        var persisted = uploadBatchRepository.findById(result.batchId()).orElseThrow();
        assertThat(persisted.getFilename()).isEqualTo("dues.csv");
        assertThat(persisted.getInstitutionId()).isEqualTo(institutionId);
        assertThat(persisted.getFileChecksum()).hasSize(64);
        assertThat(persisted.getErrors()).hasSize(1);
        assertThat(persisted.getErrors().get(0).getReason()).contains("Unknown fee type");
    }

    @Test
    void submittingAllGoodRowsMarksBatchCompleted() {
        UUID institutionId = seedInstitution();
        UUID uploadedBy = seedInstitutionUser(institutionId);

        InputStream file = csv("29901234567890,TUITION,5000,EGP," + futurePeriod + ",19001112223344,Ahmed Mohamed");

        FeeUploadResult result = feeUploadService.submit(institutionId, uploadedBy, "dues.csv", file);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.errorCount()).isZero();
    }

    @Test
    void submittingAllBadRowsMarksBatchFailed() {
        UUID institutionId = seedInstitution();
        UUID uploadedBy = seedInstitutionUser(institutionId);

        InputStream file = csv("29901234567890,SWIMMING,500,EGP," + futurePeriod + ",19001112223344,Ahmed Mohamed");

        FeeUploadResult result = feeUploadService.submit(institutionId, uploadedBy, "dues.csv", file);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.acceptedRows()).isEmpty();
    }

    @Test
    void sameFileForSameInstitutionReturnsExistingBatchAsDuplicate() {
        UUID institutionId = seedInstitution();
        UUID uploadedBy = seedInstitutionUser(institutionId);
        String line = "29901234567890,TUITION,5000,EGP," + futurePeriod
                + ",19001112223344,Ahmed Mohamed";

        FeeUploadResult first = feeUploadService.submit(
                institutionId, uploadedBy, "first.csv", "EXT-100", csv(line));

        assertThatThrownBy(() -> feeUploadService.submit(
                institutionId, uploadedBy, "renamed.csv", "EXT-101", csv(line)))
                .isInstanceOf(DuplicateUploadException.class)
                .satisfies(error -> {
                    DuplicateUploadException duplicate = (DuplicateUploadException) error;
                    assertThat(duplicate.existingBatchId()).isEqualTo(first.batchId());
                    assertThat(duplicate.existingStatus()).isEqualTo("COMPLETED");
                    assertThat(duplicate.matchedBy()).isEqualTo("FILE_CHECKSUM");
                });
    }

    @Test
    void reusedExternalReferenceIsDuplicateEvenWhenFileChanged() {
        UUID institutionId = seedInstitution();
        UUID uploadedBy = seedInstitutionUser(institutionId);

        FeeUploadResult first = feeUploadService.submit(
                institutionId,
                uploadedBy,
                "first.csv",
                "EXT-200",
                csv("29901234567890,TUITION,5000,EGP," + futurePeriod
                        + ",19001112223344,Ahmed Mohamed"));

        assertThatThrownBy(() -> feeUploadService.submit(
                institutionId,
                uploadedBy,
                "changed.csv",
                "EXT-200",
                csv("30001234567890,TUITION,6000,EGP," + futurePeriod
                        + ",19001112223344,Ahmed Mohamed")))
                .isInstanceOf(DuplicateUploadException.class)
                .satisfies(error -> {
                    DuplicateUploadException duplicate = (DuplicateUploadException) error;
                    assertThat(duplicate.existingBatchId()).isEqualTo(first.batchId());
                    assertThat(duplicate.matchedBy()).isEqualTo("EXTERNAL_BATCH_REFERENCE");
                });
    }

    @Test
    void sameFileCanBeSubmittedByDifferentInstitutions() {
        UUID firstInstitution = seedInstitution();
        UUID secondInstitution = seedInstitution();
        String line = "29901234567890,TUITION,5000,EGP," + futurePeriod
                + ",19001112223344,Ahmed Mohamed";

        FeeUploadResult first = feeUploadService.submit(
                firstInstitution, seedInstitutionUser(firstInstitution), "dues.csv", "EXT-1", csv(line));
        FeeUploadResult second = feeUploadService.submit(
                secondInstitution, seedInstitutionUser(secondInstitution), "dues.csv", "EXT-1", csv(line));

        assertThat(second.batchId()).isNotEqualTo(first.batchId());
        assertThat(second.fileChecksum()).isEqualTo(first.fileChecksum());
    }

    @Test
    void databaseContainsBothUploadUniquenessConstraints() {
        Integer constraintCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_constraint
                WHERE conname IN (
                    'uq_upload_batch_institution_checksum',
                    'uq_upload_batch_institution_external_ref')
                """, Integer.class);

        assertThat(constraintCount).isEqualTo(2);
    }
}
