package com.tuitionnetwork.tuitionnetwork.integration.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.infra.FeeRepository;
import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.service.NationalIdHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties =
        "security.national-id.hmac-key-base64=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=")
@Transactional
class CsvBillingPersistenceIntegrationTest {

    private static final String STUDENT_NATIONAL_ID = "29901234567890";

    @Autowired
    private FeeUploadService feeUploadService;

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NationalIdHasher nationalIdHasher;

    @Test
    void acceptedCsvRowIsPersistedAsBillingFee() {
        Fixture fixture = seedFixture();
        String period = YearMonth.now().plusMonths(1).toString();
        String csv = validCsv(period);

        FeeUploadResult result = feeUploadService.submit(
                fixture.institutionId(),
                fixture.uploaderId(),
                "fees.csv",
                "CSV-BILLING-1",
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        feeRepository.flush();

        var fees = feeRepository.findBySourceBatchId(result.batchId());
        assertThat(fees).hasSize(1);
        assertThat(fees.get(0).getStudentId()).isEqualTo(fixture.studentId());
        assertThat(fees.get(0).getInstitutionId()).isEqualTo(fixture.institutionId());
        assertThat(fees.get(0).getAmount()).isEqualByComparingTo("5000");
        assertThat(fees.get(0).getStatus()).isEqualTo(FeeStatus.OUTSTANDING);
    }

    @Test
    void duplicateCsvDoesNotCreateASecondBillingFee() {
        Fixture fixture = seedFixture();
        String csv = validCsv(YearMonth.now().plusMonths(1).toString());

        FeeUploadResult original = submit(fixture, "CSV-BILLING-DUPLICATE-1", csv);
        feeRepository.flush();

        assertThatThrownBy(() -> submit(fixture, "CSV-BILLING-DUPLICATE-2", csv))
                .isInstanceOfSatisfying(DuplicateUploadException.class, duplicate -> {
                    assertThat(duplicate.existingBatchId()).isEqualTo(original.batchId());
                    assertThat(duplicate.matchedBy()).isEqualTo("FILE_CHECKSUM");
                });

        var persistedFees = feeRepository.findByInstitutionId(fixture.institutionId());
        assertThat(persistedFees).hasSize(1);
        assertThat(persistedFees.get(0).getSourceBatchId()).isEqualTo(original.batchId());
    }

    private FeeUploadResult submit(Fixture fixture, String externalBatchReference, String csv) {
        return feeUploadService.submit(
                fixture.institutionId(),
                fixture.uploaderId(),
                "fees.csv",
                externalBatchReference,
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private String validCsv(String period) {
        return """
                national_id,fee_type,amount,currency,collection_period,parent_national_id,parent_full_name
                %s,TUITION,5000,EGP,%s,19001112223344,Ahmed Mohamed
                """.formatted(STUDENT_NATIONAL_ID, period);
    }

    private Fixture seedFixture() {
        UUID institutionId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO billing.institution (id, name, type, status) VALUES (?, ?, ?, ?)",
                institutionId, "CSV Billing School", "SCHOOL", "ACTIVE");

        UUID roleId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity.role (id, name, description) VALUES (?, ?, ?)",
                roleId, "CSV_" + roleId.toString().substring(0, 8), "Test role");

        UUID uploaderId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity.institution_user (id, institution_id, full_name, role_id) VALUES (?, ?, ?, ?)",
                uploaderId, institutionId, "CSV Test Uploader", roleId);

        UUID studentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity.student (id, national_id_hmac, full_name) VALUES (?, ?, ?)",
                studentId,
                nationalIdHasher.hash(new NationalId(STUDENT_NATIONAL_ID)),
                "CSV Test Student");

        return new Fixture(institutionId, uploaderId, studentId);
    }

    private record Fixture(UUID institutionId, UUID uploaderId, UUID studentId) {
    }
}
