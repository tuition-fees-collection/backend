package com.tuitionnetwork.tuitionnetwork.identity.infra;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.service.NationalIdHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "security.national-id.hmac-key-base64=YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY=",
        "security.national-id.encryption-key-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@Transactional
class GuardianProvisioningAdapterPersistenceTest {

    @Autowired private GuardianProvisioningAdapter adapter;
    @Autowired private NationalIdHasher hasher;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void upsertIsIdempotentAndLinksGuardianToStudentOnce() {
        String guardianNationalId = "29801011234567";
        UUID studentId = seedStudent();

        UUID first = adapter.upsertGuardianStub(guardianNationalId, "Ahmed Mohamed");
        UUID replay = adapter.upsertGuardianStub(guardianNationalId, "Ahmed M. Mohamed");
        adapter.linkGuardianToStudent(first, studentId);
        adapter.linkGuardianToStudent(first, studentId);

        assertThat(replay).isEqualTo(first);
        byte[] encrypted = jdbc.queryForObject(
                "SELECT national_id_encrypted FROM identity.guardian WHERE id = ?",
                byte[].class, first);
        assertThat(new String(encrypted, StandardCharsets.UTF_8)).doesNotContain(guardianNationalId);
        Integer links = jdbc.queryForObject(
                "SELECT COUNT(*) FROM identity.guardian_student WHERE guardian_id = ? AND student_id = ?",
                Integer.class, first, studentId);
        assertThat(links).isEqualTo(1);
    }

    private UUID seedStudent() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO identity.student (id, national_id_hmac, full_name) VALUES (?, ?, ?)",
                id, hasher.hash(new NationalId("29901234567890")), "Student One");
        return id;
    }
}
