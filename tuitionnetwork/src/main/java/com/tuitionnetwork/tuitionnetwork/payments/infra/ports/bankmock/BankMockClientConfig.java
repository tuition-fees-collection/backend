package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;


@Configuration
public class BankMockClientConfig {

    @Bean
    public RestClient bankMockRestClient(
            @Value("${bankmock.base-url}") String baseUrl,
            @Value("${bankmock.api-key}") String apiKey) {
        ObjectMapper objectMapper = new ObjectMapper();
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    JsonNode body;
                    try {
                        body = objectMapper.readTree(response.getBody());
                    } catch (IOException e) {
                        throw new BankMockApiException("UNKNOWN", "Unreadable error response from bank mock", null);
                    }
                    JsonNode error = body.path("error");
                    if (!error.isMissingNode() && !error.isNull()) {
                        Map<String, Object> details = objectMapper.convertValue(
                                error.path("details"),
                                new com.fasterxml.jackson.core.type.TypeReference<>() { });
                        throw new BankMockApiException(
                                error.path("code").asText("UNKNOWN"),
                                error.path("message").asText("Bank mock request failed"),
                                details);
                    }

                    if (body.has("detail")) {
                        throw new BankMockApiException(
                                "VALIDATION_ERROR",
                                "Bank mock rejected the request",
                                Map.of("detail", objectMapper.convertValue(body.get("detail"), Object.class)));
                    }

                    throw new BankMockApiException(
                            "HTTP_" + response.getStatusCode().value(),
                            "Bank mock request failed",
                            Map.of());
                })
                .build();
    }
}
