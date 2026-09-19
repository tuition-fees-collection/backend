package com.tuitionnetwork.tuitionnetwork.billing.infra;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class BillingInstitutionLookupRepository {
    private final JdbcClient jdbcClient;

    public BillingInstitutionLookupRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Map<UUID, String> findNamesByIds(Collection<UUID> institutionIds) {
        if (institutionIds == null || institutionIds.isEmpty()) {
            return Map.of();
        }
        var rows = jdbcClient.sql("""
                        SELECT id, name
                        FROM billing.institution
                        WHERE id IN (:ids)
                        """)
                .param("ids", List.copyOf(institutionIds))
                .query((rs, rowNum) -> Map.entry(
                        rs.getObject("id", UUID.class),
                        rs.getString("name")))
                .list();
        return rows.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }
}
