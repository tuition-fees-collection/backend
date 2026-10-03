package com.tuitionnetwork.tuitionnetwork.chatbot.service;

public class ChatbotQueryException extends RuntimeException {
    public ChatbotQueryException() {
        super("internal_error");
    }
}