package com.tuitionnetwork.tuitionnetwork.chatbot.service;

public record ChatbotDecision(String functionName, String directReply) {

    public boolean hasFunction() {
        return functionName != null && !functionName.isBlank();
    }
}
