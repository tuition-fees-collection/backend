package com.tuitionnetwork.tuitionnetwork.chatbot.api;

import com.tuitionnetwork.tuitionnetwork.chatbot.service.ChatbotService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    public ChatbotAskResponse ask(@Valid @RequestBody ChatbotAskRequest request) {
        return chatbotService.ask(request.message());
    }
}
