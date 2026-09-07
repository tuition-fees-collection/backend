package com.tuitionnetwork.tuitionnetwork.epp.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuitionnetwork.tuitionnetwork.epp.service.EppPlanNotFoundException;
import com.tuitionnetwork.tuitionnetwork.epp.service.EppService;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BankMockApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EppController.class)
@WithMockUser
class EppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EppService eppService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String body(UUID paymentId) throws Exception {
        return objectMapper.writeValueAsString(new CreateEppPlanRequest(paymentId, "pay-1", 24, "Books"));
    }

    @Test
    void createPlanWithUnsupportedTenorReturns400() throws Exception {
        when(eppService.createPlan(any(), anyString(), anyInt(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Unsupported tenor: 24 (allowed: [3, 6, 12, 18])"));

        mockMvc.perform(post("/api/v1/epp/plans")
                        .with(csrf())
                        .header("Idempotency-Key", "idem-1")
                        .contentType("application/json")
                        .content(body(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void createPlanWhenPaymentAlreadyHasAPlanReturns409() throws Exception {
        when(eppService.createPlan(any(), anyString(), anyInt(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Payment already has an EPP plan"));

        mockMvc.perform(post("/api/v1/epp/plans")
                        .with(csrf())
                        .header("Idempotency-Key", "idem-1")
                        .contentType("application/json")
                        .content(body(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void createPlanWhenGatewayRejectsReturns502() throws Exception {
        when(eppService.createPlan(any(), anyString(), anyInt(), anyString(), anyString()))
                .thenThrow(new BankMockApiException("CARD_NOT_ELIGIBLE", "Card not eligible for EPP", Map.of()));

        mockMvc.perform(post("/api/v1/epp/plans")
                        .with(csrf())
                        .header("Idempotency-Key", "idem-1")
                        .contentType("application/json")
                        .content(body(UUID.randomUUID())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CARD_NOT_ELIGIBLE"));
    }

    @Test
    void getPlanWhenMissingReturns404() throws Exception {
        UUID planId = UUID.randomUUID();
        when(eppService.getPlan(planId)).thenThrow(new EppPlanNotFoundException(planId));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/epp/plans/{id}", planId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
