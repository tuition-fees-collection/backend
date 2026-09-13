package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.BackofficeEmployeeResponse;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BackofficeAdminRepository {

    private final JdbcClient jdbcClient;

    public BackofficeAdminRepository(
            JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<UUID> findRoleId(
            RoleCode role
    ) {

        return jdbcClient.sql("""
                SELECT id
                FROM identity.role
                WHERE name = :role
                """)
                .param("role", role.name())
                .query(UUID.class)
                .optional();
    }

    public UUID createEmployee(
            String fullName,
            String branch,
            UUID roleId
    ) {

        return jdbcClient.sql("""
                INSERT INTO identity.backoffice_employee (
                    full_name,
                    branch,
                    role_id
                )
                VALUES (
                    :fullName,
                    :branch,
                    :roleId
                )
                RETURNING id
                """)
                .param("fullName", fullName)
                .param("branch", branch)
                .param("roleId", roleId)
                .query(UUID.class)
                .single();
    }

    public boolean existsById(
            UUID employeeId
    ) {

        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM identity.backoffice_employee
                    WHERE id = :employeeId
                )
                """)
                .param("employeeId", employeeId)
                .query(Boolean.class)
                .single();
    }

    public void updateRole(
            UUID employeeId,
            UUID roleId
    ) {

        jdbcClient.sql("""
                UPDATE identity.backoffice_employee
                SET role_id = :roleId
                WHERE id = :employeeId
                """)
                .param("roleId", roleId)
                .param("employeeId", employeeId)
                .update();
    }

    public List<BackofficeEmployeeResponse>
    findAll() {

        return jdbcClient.sql("""
                SELECT
                    e.id AS employee_id,
                    e.full_name,
                    e.branch,
                    r.name AS role,
                    a.id AS account_id,
                    a.username_normalized,
                    a.status AS account_status
                FROM identity.backoffice_employee e

                JOIN identity.role r
                    ON r.id = e.role_id

                LEFT JOIN identity.auth_account a
                    ON a.backoffice_employee_id = e.id

                ORDER BY e.full_name
                """)
                .query((rs, rowNum) ->
                        new BackofficeEmployeeResponse(
                                rs.getObject(
                                        "employee_id",
                                        UUID.class
                                ),
                                rs.getString(
                                        "full_name"
                                ),
                                rs.getString(
                                        "branch"
                                ),
                                RoleCode.valueOf(
                                        rs.getString(
                                                "role"
                                        )
                                ),
                                rs.getObject(
                                        "account_id",
                                        UUID.class
                                ),
                                rs.getString(
                                        "username_normalized"
                                ),
                                rs.getString(
                                        "account_status"
                                )
                        )
                )
                .list();
    }
}
