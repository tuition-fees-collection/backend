package com.tuitionnetwork.tuitionnetwork.epp.infra.ports;

import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppCreateRequest;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppPlanResponse;
import com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock.EppQuoteResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;


@Component
public class EppGatewayAdapter implements EppGatewayPort {

    private final RestClient bankMockRestClient;

    public EppGatewayAdapter(RestClient bankMockRestClient) {
        this.bankMockRestClient = bankMockRestClient;
    }

    @Override
    public EppQuoteResponse quote(BigDecimal amount, String currency) {
        return bankMockRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/epp/quotes")
                        .queryParam("amount", amount)
                        .queryParam("currency", currency)
                        .build())
                .retrieve()
                .body(EppQuoteResponse.class);
    }

    @Override
    public EppPlanResponse create(EppCreateRequest request, String idempotencyKey) {
        return bankMockRestClient.post()
                .uri("/api/v1/epp")
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .retrieve()
                .body(EppPlanResponse.class);
    }

    @Override
    public EppPlanResponse cancel(String gatewayPlanId) {
        return bankMockRestClient.post()
                .uri("/api/v1/epp/{id}/cancel", gatewayPlanId)
                .retrieve()
                .body(EppPlanResponse.class);
    }
}
