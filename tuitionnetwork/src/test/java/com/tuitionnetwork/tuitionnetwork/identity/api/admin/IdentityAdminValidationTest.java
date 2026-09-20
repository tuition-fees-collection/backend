package com.tuitionnetwork.tuitionnetwork.identity.api.admin;

import com.tuitionnetwork.tuitionnetwork.identity.service.BackofficeAdminService;
import com.tuitionnetwork.tuitionnetwork.identity.service.InstitutionAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = {BackofficeAdminController.class, InstitutionAdminController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(IdentityAdminExceptionHandler.class)
class IdentityAdminValidationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BackofficeAdminService backofficeAdminService;

    @MockitoBean
    private InstitutionAdminService institutionAdminService;

    @Test
    @DisplayName("rejects an empty back-office officer body before the service is called")
    void emptyBackofficeBody() throws Exception {
        mvc.perform(post("/api/v1/admin/backoffice-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.fullName").value("Full name is required"))
                .andExpect(jsonPath("$.fieldErrors.branch").value("Branch is required"))
                .andExpect(jsonPath("$.fieldErrors.role").value("Role is required"))
                .andExpect(jsonPath("$.fieldErrors.username").value("Username is required"))
                .andExpect(jsonPath("$.fieldErrors.password").value("Password is required"));

        verify(backofficeAdminService, never()).create(any());
    }

    @Test
    @DisplayName("rejects a short officer password and invalid username")
    void invalidBackofficeCredentials() throws Exception {
        mvc.perform(post("/api/v1/admin/backoffice-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Mariam Ezzat",
                                  "branch": "zamalek",
                                  "role": "BANK_OFFICER",
                                  "username": "ab",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be at least 8 characters"));

        verify(backofficeAdminService, never()).create(any());
    }

    @Test
    @DisplayName("rejects an empty institution body before the service is called")
    void emptyInstitutionBody() throws Exception {
        mvc.perform(post("/api/v1/admin/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Institution name is required"))
                .andExpect(jsonPath("$.fieldErrors.type").value("Institution type is required"))
                .andExpect(jsonPath("$.fieldErrors.status").value("Status is required"));

        verify(institutionAdminService, never()).createInstitution(any());
    }

    @Test
    @DisplayName("rejects an unknown institution type")
    void invalidInstitutionType() throws Exception {
        mvc.perform(post("/api/v1/admin/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nile International School",
                                  "type": "COLLEGE",
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.type")
                        .value("Type must be SCHOOL, UNIVERSITY, or INSTITUTE"));

        verify(institutionAdminService, never()).createInstitution(any());
    }

    @Test
    @DisplayName("rejects an empty institution officer body before the service is called")
    void emptyInstitutionUserBody() throws Exception {
        UUID institutionId = UUID.randomUUID();

        mvc.perform(post("/api/v1/admin/institutions/" + institutionId + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.fullName").value("Contact name is required"))
                .andExpect(jsonPath("$.fieldErrors.username").value("Username is required"))
                .andExpect(jsonPath("$.fieldErrors.password").value("Password is required"));

        verify(institutionAdminService, never()).createInstitutionUser(any(), any());
    }
}
