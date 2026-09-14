package com.tuitionnetwork.tuitionnetwork.payments.infra.ports;

import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BackofficePaymentRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BackofficePaymentResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CaptureRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class BackofficePaymentAdapter implements BackofficePaymentPort {

    private final RestClient bankMockRestClient;

    public BackofficePaymentAdapter(RestClient bankMockRestClient) {
        this.bankMockRestClient = bankMockRestClient;
    }

    @Override
    public BackofficePaymentResponse charge(BackofficePaymentRequest request, String idempotencyKey) {
        return bankMockRestClient.post()
                .uri("/api/v1/backoffice/payments")
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .retrieve()
                .body(BackofficePaymentResponse.class);
    }

    @Override
    public BackofficePaymentResponse capture(String paymentId, BigDecimal amount) {
        return bankMockRestClient.post()
                .uri("/api/v1/backoffice/payments/{id}/capture", paymentId)
                .body(new CaptureRequest(amount))
                .retrieve()
                .body(BackofficePaymentResponse.class);
    }

    @Override
    public BackofficePaymentResponse getStatus(String paymentId) {
        return bankMockRestClient.get()
                .uri("/api/v1/backoffice/payments/{id}", paymentId)
                .retrieve()
                .body(BackofficePaymentResponse.class);
    }

    @Override
    public List<BackofficePaymentResponse> list(String customerId, String sourceId, String status) {
        return bankMockRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/backoffice/payments")
                        .queryParamIfPresent("customer_id", Optional.ofNullable(customerId))
                        .queryParamIfPresent("source_id", Optional.ofNullable(sourceId))
                        .queryParamIfPresent("status", Optional.ofNullable(status))
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<BackofficePaymentResponse>>() { });
    }
}
