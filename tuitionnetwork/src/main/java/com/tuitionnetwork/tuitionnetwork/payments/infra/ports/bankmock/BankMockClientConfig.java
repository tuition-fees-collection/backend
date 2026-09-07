package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.io.IOException;


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
                    BankMockErrorResponse body;
                    try {
                        body = objectMapper.readValue(response.getBody(), BankMockErrorResponse.class);
                    } catch (IOException e) {
                        throw new BankMockApiException("UNKNOWN", "Unreadable error response from bank mock", null);
                    }
                    BankMockError error = body.error();
                    throw new BankMockApiException(error.code(), error.message(), error.details());
                })
                .build();
    }
}
