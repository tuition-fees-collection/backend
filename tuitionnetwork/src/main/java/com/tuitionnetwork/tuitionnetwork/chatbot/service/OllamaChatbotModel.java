package com.tuitionnetwork.tuitionnetwork.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OllamaChatbotModel implements ChatbotModel {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatbotModel.class);

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String model;

    public OllamaChatbotModel(
            @Value("${chatbot.ollama.url}") String baseUrl,
            @Value("${chatbot.ollama.model}") String model
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.objectMapper = new ObjectMapper();
        this.model = model;
    }

    @Override
    public ChatbotDecision classify(String message) {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", ChatbotPrompts.SYSTEM),
                        Map.of("role", "user", "content", message)
                ),
                "tools", ChatbotPrompts.TOOLS,
                "options", Map.of("temperature", 0)
        );
        try {
            String raw = client.post()
                    .uri("/api/chat")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw == null ? "{}" : raw);
            return OllamaResponseParser.parse(root);
        } catch (RestClientException ex) {
            log.warn("Ollama request failed");
            throw new ChatbotModelUnavailableException();
        } catch (Exception ex) {
            log.warn("Ollama response could not be read");
            throw new ChatbotModelUnavailableException();
        }
    }
}
