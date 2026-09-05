package com.tuitionnetwork.tuitionnetwork;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc
class TuitionnetworkApplicationTests {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MockMvc mockMvc;

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
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
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
