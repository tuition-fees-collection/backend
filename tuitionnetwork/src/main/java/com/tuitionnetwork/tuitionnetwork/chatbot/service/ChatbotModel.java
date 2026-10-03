package com.tuitionnetwork.tuitionnetwork.chatbot.service;

public interface ChatbotModel {
    ChatbotDecision classify(String message);
}
