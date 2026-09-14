package com.tuitionnetwork.tuitionnetwork.identity.infra;

import com.tuitionnetwork.tuitionnetwork.identity.domain.NationalId;
import com.tuitionnetwork.tuitionnetwork.identity.service.NationalIdEncryptor;
import com.tuitionnetwork.tuitionnetwork.identity.service.NationalIdHasher;
import com.tuitionnetwork.tuitionnetwork.integration.infra.ports.GuardianProvisioningPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GuardianProvisioningAdapter implements GuardianProvisioningPort {

    private final JdbcClient jdbcClient;
    private final NationalIdHasher nationalIdHasher;
    private final NationalIdEncryptor nationalIdEncryptor;

    public GuardianProvisioningAdapter(JdbcClient jdbcClient,
                                       NationalIdHasher nationalIdHasher,
                                       NationalIdEncryptor nationalIdEncryptor) {
        this.jdbcClient = jdbcClient;
        this.nationalIdHasher = nationalIdHasher;
        this.nationalIdEncryptor = nationalIdEncryptor;
    }

    @Override
    public UUID upsertGuardianStub(String nationalIdValue, String fullName) {
        NationalId nationalId = new NationalId(nationalIdValue);
        String normalizedName = requireFullName(fullName);
        String hmac = nationalIdHasher.hash(nationalId);
        byte[] encrypted = nationalIdEncryptor.encrypt(nationalId);

        return jdbcClient.sql("""
                        INSERT INTO identity.guardian
                            (national_id_hmac, national_id_encrypted, full_name, verified)
                        VALUES (:hmac, :encrypted, :fullName, FALSE)
                        ON CONFLICT (national_id_hmac) DO UPDATE
                        SET full_name = CASE
                            WHEN identity.guardian.verified THEN identity.guardian.full_name
                            ELSE EXCLUDED.full_name
                        END
                        RETURNING id
                        """)
                .param("hmac", hmac)
                .param("encrypted", encrypted)
                .param("fullName", normalizedName)
                .query(UUID.class)
                .single();
    }

    @Override
    public void linkGuardianToStudent(UUID guardianId, UUID studentId) {
        jdbcClient.sql("""
                        INSERT INTO identity.guardian_student (guardian_id, student_id, source)
                        VALUES (:guardianId, :studentId, 'INSTITUTION_IMPORT')
                        ON CONFLICT (guardian_id, student_id) DO NOTHING
                        """)
                .param("guardianId", guardianId)
                .param("studentId", studentId)
                .update();
    }

    private String requireFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Guardian full name is required");
        }
        return fullName.trim();
    }
}
