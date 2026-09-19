package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public Map<UUID, String> findFullNamesByIds(Collection<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }
        var rows = jdbcClient.sql("""
                        SELECT id, full_name
                        FROM identity.student
                        WHERE id IN (:ids)
                        """)
                .param("ids", List.copyOf(studentIds))
                .query((rs, rowNum) -> Map.entry(
                        rs.getObject("id", UUID.class),
                        rs.getString("full_name")))
                .list();
        return rows.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }
}
