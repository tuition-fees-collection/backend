package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.*;
import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InstitutionAdminPersistenceTest {
    @Autowired InstitutionAdminService service;
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    @Test void createsInstitutionAndOfficerWhoCanLoginWithInstitutionScope() {
        UUID admin = UUID.randomUUID();
        UUID institution = null;
        UUID user = null;
        String username = "institution-test-" + UUID.randomUUID();
        jdbc.update("INSERT INTO identity.backoffice_employee(id, full_name, role_id) SELECT ?, 'Test admin', id FROM identity.role WHERE name = 'ADMIN'", admin);
        var jwt = Jwt.withTokenValue("trusted-test-admin").header("alg", "HS256").subject(admin.toString())
                .claim("actor_id", admin.toString()).claim("actor_type", "BACKOFFICE_EMPLOYEE").claim("role", "ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        try {
            var created = service.createInstitution(new CreateInstitutionRequest("Test School", "SCHOOL", "ACTIVE"));
            institution = created.id();
            assertEquals(admin, jdbc.queryForObject("SELECT created_by FROM billing.institution WHERE id = ?", UUID.class, institution));
            assertTrue(service.getInstitutions().contains(created));
            var updated = service.updateInstitution(institution, new UpdateInstitutionRequest("Updated School", null, null));
            assertEquals("Updated School", updated.name());
            assertEquals("SCHOOL", updated.type());
            var officer = service.createInstitutionUser(institution, new CreateInstitutionUserRequest("Test Officer", username, "test-password"));
            user = officer.userId();
            assertNotNull(officer.accountId());
            assertEquals(institution, officer.institutionId());
            assertTrue(service.getInstitutionUsers(institution).contains(officer));
            var login = auth.authenticateCredentials(username, "test-password");
            assertTrue(login.authenticated());
            assertEquals(RoleCode.INSTITUTION_OFFICER, login.actor().role());
            assertEquals(institution, login.actor().institutionId());

            UUID finalInstitution = institution;
            // Both JDBC profile and JPA account must join the same transaction.
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                service.createInstitutionUser(finalInstitution,
                        new CreateInstitutionUserRequest("Rollback Officer", username + "-rollback", "test-password"));
                status.setRollbackOnly();
            });
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM identity.auth_account WHERE username_normalized = ?",
                    Integer.class, username + "-rollback"));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM identity.institution_user WHERE institution_id = ?",
                    Integer.class, institution));
        } finally {
            SecurityContextHolder.clearContext();
            jdbc.update("DELETE FROM common.audit_log WHERE actor_id = ? OR actor_id = ?", admin, user);
            jdbc.update("DELETE FROM identity.auth_account WHERE username_normalized IN (?, ?)", username, username + "-rollback");
            if (institution != null) {
                jdbc.update("DELETE FROM identity.institution_user WHERE institution_id = ?", institution);
                jdbc.update("DELETE FROM billing.institution WHERE id = ?", institution);
            }
            jdbc.update("DELETE FROM identity.backoffice_employee WHERE id = ?", admin);
        }
    }
}
