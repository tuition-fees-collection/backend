package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public class GuardianIdentityLookupRepository {

    private final JdbcClient jdbcClient;

    public GuardianIdentityLookupRepository(
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    public java.util.Optional<UUID>
    findGuardianIdByNationalIdHmac(
            String nationalIdHmac
    ) {

        return jdbcClient.sql("""
                SELECT id
                FROM identity.guardian
                WHERE national_id_hmac = :hmac
                """)
                .param(
                        "hmac",
                        nationalIdHmac
                )
                .query(UUID.class)
                .optional();
    }

    public Set<UUID> findStudentIdsByGuardianId(
            UUID guardianId
    ) {

        var ids =
                jdbcClient.sql("""
                        SELECT student_id
                        FROM identity.guardian_student
                        WHERE guardian_id = :guardianId
                        """)
                        .param(
                                "guardianId",
                                guardianId
                        )
                        .query(UUID.class)
                        .list();

        return Set.copyOf(ids);
    }
}
