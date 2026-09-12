package com.tuitionnetwork.tuitionnetwork.identity.service;

import com.tuitionnetwork.tuitionnetwork.identity.api.admin.*;
import com.tuitionnetwork.tuitionnetwork.identity.domain.*;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.TokenIssuer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackofficeAdminPersistenceTest {
    @Autowired BackofficeAdminService service;
    @Autowired AuthService authentication;
    @Autowired TokenIssuer issuer;
    @Autowired JwtDecoder decoder;
    @Autowired JdbcTemplate jdbc;

    @Test
    void newStaffCanLoginAndRoleChangeRevokesOldToken() {
        UUID adminId = UUID.randomUUID();
        String username = "staff-test-" + UUID.randomUUID();
        UUID employeeId = null;
        var jwt = Jwt.withTokenValue("trusted-test-admin").header("alg", "HS256")
                .subject(adminId.toString()).claim("actor_id", adminId.toString())
                .claim("actor_type", "BACKOFFICE_EMPLOYEE").claim("role", "ADMIN").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        try {
            var created = service.create(new CreateBackofficeEmployeeRequest("Test Employee", "Branch",
                    RoleCode.BANK_OFFICER, username, "local-test-password"));
            employeeId = created.employeeId();
            assertNotNull(created.accountId());
            assertTrue(service.getAll().stream().anyMatch(row -> row.accountId() != null && row.accountId().equals(created.accountId())));
            var login = authentication.authenticateCredentials(username, "local-test-password");
            assertTrue(login.authenticated());
            assertEquals(RoleCode.BANK_OFFICER, login.actor().role());
            String oldToken = issuer.issue(login.actor(), login.tokenVersion());
            assertDoesNotThrow(() -> decoder.decode(oldToken));
            service.changeRole(employeeId, new ChangeBackofficeRoleRequest(RoleCode.ADMIN));
            assertThrows(JwtValidationException.class, () -> decoder.decode(oldToken));
            var newLogin = authentication.authenticateCredentials(username, "local-test-password");
            assertEquals(RoleCode.ADMIN, newLogin.actor().role());
            assertEquals(1L, newLogin.tokenVersion());
            assertDoesNotThrow(() -> decoder.decode(issuer.issue(newLogin.actor(), newLogin.tokenVersion())));
        } finally {
            SecurityContextHolder.clearContext();
            jdbc.update("DELETE FROM common.audit_log WHERE actor_id = ? OR actor_id = ?", adminId, employeeId);
            jdbc.update("DELETE FROM identity.auth_account WHERE username_normalized = ?", username);
            if (employeeId != null) jdbc.update("DELETE FROM identity.backoffice_employee WHERE id = ?", employeeId);
        }
    }
}
