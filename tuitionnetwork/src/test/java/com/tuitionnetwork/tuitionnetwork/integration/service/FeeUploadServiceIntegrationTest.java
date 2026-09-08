package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.integration.infra.UploadBatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
class FeeUploadServiceIntegrationTest {

    @Autowired
    private FeeUploadService feeUploadService;

    @Autowired
    private UploadBatchRepository uploadBatchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
}
