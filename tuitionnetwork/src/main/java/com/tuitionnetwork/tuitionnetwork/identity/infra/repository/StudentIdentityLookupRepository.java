package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class StudentIdentityLookupRepository {

    private final JdbcClient jdbcClient;

    public StudentIdentityLookupRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<UUID> findStudentIdByNationalIdHmac(String nationalIdHmac) {
        return jdbcClient.sql("""
                        SELECT id
                        FROM identity.student
                        WHERE national_id_hmac = :nationalIdHmac
                        """)
                .param("nationalIdHmac", nationalIdHmac)
                .query(UUID.class)
                .optional();
    }
}
