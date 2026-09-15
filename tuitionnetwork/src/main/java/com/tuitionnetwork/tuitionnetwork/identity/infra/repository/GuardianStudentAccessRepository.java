package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class GuardianStudentAccessRepository {
    private final JdbcClient jdbcClient;

    public GuardianStudentAccessRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean existsRelationship(UUID guardianId, UUID studentId) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM identity.guardian_student
                    WHERE guardian_id = :guardianId
                      AND student_id = :studentId
                )
                """)
                .param("guardianId", guardianId)
                .param("studentId", studentId)
                .query(Boolean.class)
                .single();
    }

    public Optional<UUID> findFirstGuardianIdByStudentId(UUID studentId) {
        return jdbcClient.sql("""
                SELECT guardian_id
                FROM identity.guardian_student
                WHERE student_id = :studentId
                ORDER BY guardian_id
                LIMIT 1
                """)
                .param("studentId", studentId)
                .query(UUID.class)
                .optional();
    }
}
