package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;

final class OllamaResponseParser {

    private OllamaResponseParser() {
    }

    static ChatbotDecision parse(JsonNode root) {
        JsonNode message = root == null ? null : root.path("message");
        JsonNode calls = message == null ? null : message.path("tool_calls");
        if (calls != null && calls.isArray() && !calls.isEmpty()) {
            String name = calls.get(0).path("function").path("name").asText("");
            if (!name.isBlank()) {
                return new ChatbotDecision(name, null);
            }
        }
        String content = message == null ? "" : message.path("content").asText("");
        return new ChatbotDecision(null, content);
    }
}
