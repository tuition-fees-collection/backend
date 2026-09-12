package com.tuitionnetwork.tuitionnetwork.identity.infra.repository;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class InstitutionAdminRepository {
    private final JdbcClient jdbc;
    public InstitutionAdminRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<InstitutionResponse> findAllInstitutions() {
        return jdbc.sql("SELECT id, name, type, status FROM billing.institution ORDER BY name, id")
                .query(InstitutionResponse.class).list();
    }
    public Optional<InstitutionResponse> findInstitutionById(UUID id) {
        return jdbc.sql("SELECT id, name, type, status FROM billing.institution WHERE id = :id")
                .param("id", id).query(InstitutionResponse.class).optional();
    }
    public UUID createInstitution(String name, String type, String status, UUID createdBy) {
        return jdbc.sql("""
                INSERT INTO billing.institution(name, type, status, created_by)
                VALUES (:name, :type, :status, :createdBy) RETURNING id
                """).param("name", name).param("type", type).param("status", status)
                .param("createdBy", createdBy).query(UUID.class).single();
    }
    public void updateInstitution(UUID id, String name, String type, String status) {
        int updated = jdbc.sql("""
                UPDATE billing.institution SET name = COALESCE(:name, name),
                    type = COALESCE(:type, type), status = COALESCE(:status, status)
                WHERE id = :id
                """).param("name", name, java.sql.Types.VARCHAR).param("type", type, java.sql.Types.VARCHAR)
                .param("status", status, java.sql.Types.VARCHAR).param("id", id).update();
        if (updated != 1) throw new IllegalArgumentException("Institution not found");
    }
    public boolean institutionExists(UUID id) { return findInstitutionById(id).isPresent(); }
    public UUID createInstitutionUser(UUID institutionId, String fullName, UUID roleId) {
        return jdbc.sql("""
                INSERT INTO identity.institution_user(institution_id, full_name, role_id)
                VALUES (:institutionId, :fullName, :roleId) RETURNING id
                """).param("institutionId", institutionId).param("fullName", fullName)
                .param("roleId", roleId).query(UUID.class).single();
    }
    public List<InstitutionUserResponse> findUsers(UUID institutionId) {
        return jdbc.sql("""
                SELECT u.id AS user_id, u.institution_id, u.full_name, r.name AS role,
                    a.id AS account_id, a.username_normalized AS username, a.status AS account_status
                FROM identity.institution_user u JOIN identity.role r ON r.id = u.role_id
                LEFT JOIN identity.auth_account a ON a.institution_user_id = u.id
                WHERE u.institution_id = :id ORDER BY u.full_name, u.id
                """).param("id", institutionId).query(InstitutionUserResponse.class).list();
    }
    public Optional<UUID> findInstitutionOfficerRoleId() {
        return jdbc.sql("SELECT id FROM identity.role WHERE name = 'INSTITUTION_OFFICER'")
                .query(UUID.class).optional();
    }
}
