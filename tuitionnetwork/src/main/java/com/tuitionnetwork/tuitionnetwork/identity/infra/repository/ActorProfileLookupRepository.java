package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ActorProfileLookupRepository {

    private final JdbcClient jdbcClient;

    public ActorProfileLookupRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }
    public record BackofficeProfile(
        UUID actorId,
        String roleName,
        String branch
    ) {}
    public record InstitutionProfile(
        UUID actorId,
        UUID institutionId,
        String roleName
    ) {}
    public Optional<BackofficeProfile> findBackofficeProfile(
        UUID employeeId
    ) {
        return jdbcClient.sql("""
                SELECT
                    e.id AS actor_id,
                    r.name AS role_name,
                    e.branch
                FROM identity.backoffice_employee e
                JOIN identity.role r
                    ON r.id = e.role_id
                WHERE e.id = :employeeId
                """)
                .param("employeeId", employeeId)
                .query((rs, rowNum) ->
                        new BackofficeProfile(
                                rs.getObject("actor_id", UUID.class),
                                rs.getString("role_name"),
                                rs.getString("branch")
                        )
                )
            .optional();
    }
    public Optional<InstitutionProfile> findInstitutionProfile(
        UUID institutionUserId
    ) {
        return jdbcClient.sql("""
                SELECT
                    u.id AS actor_id,
                    u.institution_id,
                    r.name AS role_name
                FROM identity.institution_user u
                JOIN identity.role r
                    ON r.id = u.role_id
                WHERE u.id = :institutionUserId
                """)
                .param("institutionUserId", institutionUserId)
                .query((rs, rowNum) ->
                        new InstitutionProfile(
                                rs.getObject("actor_id", UUID.class),
                                rs.getObject(
                                        "institution_id",
                                        UUID.class
                                ),
                                rs.getString("role_name")
                        )
                )
                .optional();
    }
}