package com.tuitionnetwork.tuitionnetwork.chatbot.service;

public class ChatbotRateLimitedException extends RuntimeException {
    public ChatbotRateLimitedException() {
        super("rate_limited");
    }
}
