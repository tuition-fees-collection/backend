package com.tuitionnetwork.tuitionnetwork.chatbot.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotAskRequest(
        @NotBlank @Size(max = 2000) String message
) {
}
