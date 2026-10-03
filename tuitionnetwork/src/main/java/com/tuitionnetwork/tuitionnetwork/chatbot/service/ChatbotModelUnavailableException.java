package com.tuitionnetwork.tuitionnetwork.chatbot.service;

public class ChatbotModelUnavailableException extends RuntimeException {
    public ChatbotModelUnavailableException() {
        super("llm_unavailable");
    }
}
