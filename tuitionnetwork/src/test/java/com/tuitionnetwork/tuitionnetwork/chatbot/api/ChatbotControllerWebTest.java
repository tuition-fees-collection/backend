package com.tuitionnetwork.tuitionnetwork.chatbot.api;

import com.tuitionnetwork.tuitionnetwork.chatbot.service.ChatbotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatbotController.class)
@Import(ChatbotExceptionHandler.class)
class ChatbotControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatbotService chatbotService;

    @Test
    void askReturnsTheReplyForAnAuthenticatedCaller() throws Exception {
        when(chatbotService.ask("collected today")).thenReturn(
                ChatbotAskResponse.answered("4,500 EGP collected today, across 2 transactions.", "get_daily_collections", null)
        );

        mockMvc.perform(post("/api/v1/chatbot/ask")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"collected today\",\"institutionId\":\"someone-else\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.functionCalled").value("get_daily_collections"))
                .andExpect(jsonPath("$.reply").value("4,500 EGP collected today, across 2 transactions."));
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/chatbot/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"collected today\"}"))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(chatbotService);
    }

    @Test
    void deniedRoleBecomesForbidden() throws Exception {
        when(chatbotService.ask("collected today")).thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(post("/api/v1/chatbot/ask")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"collected today\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("not_authorized"));
    }

    @Test
    void blankMessageIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/chatbot/ask")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_parameters"));
    }
}
