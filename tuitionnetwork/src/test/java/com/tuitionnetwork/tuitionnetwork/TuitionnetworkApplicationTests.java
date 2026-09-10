package com.tuitionnetwork.tuitionnetwork;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class TuitionnetworkApplicationTests {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MockMvc mockMvc;

    @Autowired
    com.tuitionnetwork.tuitionnetwork.identity.service.auth.TokenIssuer tokenIssuer;

    @Autowired
    com.tuitionnetwork.tuitionnetwork.identity.infra.repository.AuthAccountRepository accounts;

    @Autowired
    com.tuitionnetwork.tuitionnetwork.identity.service.AdminAccountService adminAccounts;

    @Test
    void validBearerTokenIsRevokedByAdminDeactivation() throws Exception {
        var profileId = java.util.UUID.randomUUID();
        var adminId = java.util.UUID.randomUUID();
        java.util.UUID accountId = null;
        jdbc.update("INSERT INTO identity.backoffice_employee(id, full_name, branch, role_id) "
                + "SELECT ?, 'Test staff', 'Test branch', id FROM identity.role WHERE name = 'BANK_OFFICER'", profileId);
        try {
            var account = accounts.saveAndFlush(
                    com.tuitionnetwork.tuitionnetwork.identity.domain.AuthAccount.forBackofficeEmployee(
                            "revocation-" + profileId, "test-hash", profileId));
            accountId = account.getId();
            var actor = new com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor(
                    accountId, profileId,
                    com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType.BACKOFFICE_EMPLOYEE,
                    com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode.BANK_OFFICER, null, "Test branch");
            String token = tokenIssuer.issue(actor, 0);
            mockMvc.perform(get("/api/v1/nonexistent-test-route").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());

            // Supply a trusted admin context to exercise the real admin service and database transaction.
            var adminJwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-admin")
                    .header("alg", "HS256").subject(adminId.toString())
                    .claim("actor_id", adminId.toString()).claim("actor_type", "BACKOFFICE_EMPLOYEE")
                    .claim("role", "ADMIN").build();
            var authentication = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                    adminJwt, java.util.List.of());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
            adminAccounts.deactivate(accountId);
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            assertEquals(1, accounts.findById(accountId).orElseThrow().getTokenVersion());
            mockMvc.perform(get("/api/v1/nonexistent-test-route").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());

            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
            adminAccounts.reactivate(accountId);
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            mockMvc.perform(get("/api/v1/nonexistent-test-route").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/v1/nonexistent-test-route")
                    .header("Authorization", "Bearer " + tokenIssuer.issue(actor, 1)))
                    .andExpect(status().isNotFound());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            jdbc.update("DELETE FROM common.audit_log WHERE actor_id = ?", adminId);
            if (accountId != null) accounts.deleteById(accountId);
            jdbc.update("DELETE FROM identity.backoffice_employee WHERE id = ?", profileId);
        }
    }

    @Test
    void invalidBearerTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/fees").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

	@Test
	void openApiIsPublicAndIncludesProjectMetadata() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Tuition & Services Fees Collection Network API"))
				.andExpect(jsonPath("$.info.version").value("v1"));
		mockMvc.perform(get("/v3/api-docs/swagger-config"))
				.andExpect(status().isOk());
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/swagger-ui/index.html"));
		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui/swagger-ui-bundle.js"))
				.andExpect(status().isOk());
	}

	@Test
	void otherRoutesStillRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/fees").accept("application/json"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/").accept("text/html"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void contextLoads() {
	}

	@Test
	void databaseMigrationsAreApplied() {
		assertEquals(0, jdbc.queryForObject(
				"select count(*) from public.flyway_schema_history where success = false", Integer.class));
		assertEquals(2, jdbc.queryForObject(
				"select count(*) from public.flyway_schema_history where version in ('1', '2') and success = true",
				Integer.class));
		assertNotNull(jdbc.queryForObject("select to_regclass('identity.moi_verification')::text", String.class));
		assertNotNull(jdbc.queryForObject("select to_regclass('reporting.gateway_check_item')::text", String.class));
	}

}
