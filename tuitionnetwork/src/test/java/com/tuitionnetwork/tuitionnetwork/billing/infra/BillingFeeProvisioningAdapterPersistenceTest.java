package com.tuitionnetwork.tuitionnetwork.billing.infra;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.FeeProvisioningPort;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.StudentResolutionPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class BillingFeeProvisioningAdapterPersistenceTest {

    @Autowired
    private FeeRepository feeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsValidatedCsvFeeUsingDeveloperBBillingEntity() {
        Fixture fixture = seedFixture();
        BillingFeeProvisioningAdapter adapter = adapterResolving(fixture.studentId());

        UUID feeId = adapter.provisionFee(command(
                fixture.institutionId(), fixture.batchId(), "TUITION", "5000.00"));
        feeRepository.flush();

        Fee persisted = feeRepository.findById(feeId).orElseThrow();
        assertThat(persisted.getStudentId()).isEqualTo(fixture.studentId());
        assertThat(persisted.getInstitutionId()).isEqualTo(fixture.institutionId());
        assertThat(persisted.getFeeType()).isEqualTo(FeeType.TUITION);
        assertThat(persisted.getCollectionPeriod()).isEqualTo("2026-10");
        assertThat(persisted.getCurrency()).isEqualTo("EGP");
        assertThat(persisted.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(persisted.getAmountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(persisted.getStatus()).isEqualTo(FeeStatus.OUTSTANDING);
        assertThat(persisted.getSourceBatchId()).isEqualTo(fixture.batchId());
    }

    @Test
    void keepsAllProvisionedFeesTraceableToTheirUploadBatch() {
        Fixture fixture = seedFixture();
        BillingFeeProvisioningAdapter adapter = adapterResolving(fixture.studentId());

        adapter.provisionFee(command(fixture.institutionId(), fixture.batchId(), "TUITION", "5000"));
        adapter.provisionFee(command(fixture.institutionId(), fixture.batchId(), "BUS", "900"));
        feeRepository.flush();

        assertThat(feeRepository.findBySourceBatchId(fixture.batchId()))
                .hasSize(2)
                .extracting(Fee::getFeeType)
                .containsExactlyInAnyOrder(FeeType.TUITION, FeeType.BUS);
    }

    private BillingFeeProvisioningAdapter adapterResolving(UUID studentId) {
        StudentResolutionPort resolver = nationalId -> {
            assertThat(nationalId).isEqualTo("29901234567890");
            return studentId;
        };
        return new BillingFeeProvisioningAdapter(feeRepository, java.util.Optional.of(resolver));
    }

    private FeeProvisioningPort.Command command(
            UUID institutionId, UUID batchId, String feeType, String amount) {
        return new FeeProvisioningPort.Command(
                "29901234567890",
                institutionId,
                feeType,
                "2026-10",
                "EGP",
                new BigDecimal(amount),
                batchId);
    }

    private Fixture seedFixture() {
        UUID institutionId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO billing.institution (id, name, type, status) VALUES (?, ?, ?, ?)",
                institutionId, "Billing Adapter School", "SCHOOL", "ACTIVE");

        UUID roleId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity.role (id, name, description) VALUES (?, ?, ?)",
                roleId, "UPLOAD_" + roleId.toString().substring(0, 8), "Test role");

        UUID uploaderId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity.institution_user (id, institution_id, full_name, role_id) VALUES (?, ?, ?, ?)",
                uploaderId, institutionId, "Billing Test Uploader", roleId);

        UUID studentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO identity.student (id, national_id_hmac, full_name) VALUES (?, ?, ?)",
                studentId, UUID.randomUUID().toString().replace("-", "").repeat(2), "Test Student");

        UUID batchId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO integration.upload_batch (
                    id, institution_id, uploaded_by, filename, file_checksum,
                    external_batch_reference, uploaded_at, status, row_count, error_count)
                VALUES (?, ?, ?, ?, ?, ?, now(), ?, ?, ?)
                """,
                batchId,
                institutionId,
                uploaderId,
                "fees.csv",
                "c".repeat(64),
                "BILLING-TEST-" + batchId,
                "COMPLETED",
                1,
                0);

        return new Fixture(institutionId, studentId, batchId);
    }

    private record Fixture(UUID institutionId, UUID studentId, UUID batchId) {
    }
}
