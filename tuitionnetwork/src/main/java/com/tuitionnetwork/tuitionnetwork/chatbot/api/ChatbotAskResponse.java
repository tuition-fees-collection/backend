package com.tuitionnetwork.tuitionnetwork.chatbot.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatbotAskResponse(
        boolean success,
        String reply,
        String functionCalled,
        Object data,
        String error
) {
    public static ChatbotAskResponse answered(String reply, String functionCalled, Object data) {
        return new ChatbotAskResponse(true, reply, functionCalled, data, null);
    }

    public static ChatbotAskResponse refused(String reply) {
        return new ChatbotAskResponse(true, reply, null, null, null);
    }

    public static ChatbotAskResponse failed(String error, String reply) {
        return new ChatbotAskResponse(false, reply, null, null, error);
    }
}
