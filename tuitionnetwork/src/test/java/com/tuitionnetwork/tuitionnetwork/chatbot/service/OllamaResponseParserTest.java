package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaResponseParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void keepsOnlyTheFunctionNameAndDropsArguments() throws Exception {
        var root = mapper.readTree("""
                {
                  "message": {
                    "content": "",
                    "tool_calls": [
                      {
                        "function": {
                          "name": "get_daily_collections",
                          "arguments": { "institution_id": "other-school", "student_name": "Nada" }
                        }
                      },
                      { "function": { "name": "get_student_list", "arguments": {} } }
                    ]
                  }
                }
                """);

        ChatbotDecision decision = OllamaResponseParser.parse(root);

        assertEquals("get_daily_collections", decision.functionName());
        assertNull(decision.directReply());
    }

    @Test
    void usesFreeTextWhenNoToolIsCalled() throws Exception {
        var root = mapper.readTree("""
                { "message": { "content": "I can only report totals.", "tool_calls": [] } }
                """);

        ChatbotDecision decision = OllamaResponseParser.parse(root);

        assertFalse(decision.hasFunction());
        assertEquals("I can only report totals.", decision.directReply());
    }
}
