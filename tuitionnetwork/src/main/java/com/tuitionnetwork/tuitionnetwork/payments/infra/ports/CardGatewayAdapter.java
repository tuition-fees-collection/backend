package com.tuitionnetwork.tuitionnetwork.payments.infra.ports;

import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CaptureRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.RefundRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.ThreeDSConfirmRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;


@Component
public class CardGatewayAdapter implements CardGatewayPort {

    private final RestClient bankMockRestClient;

    public CardGatewayAdapter(RestClient bankMockRestClient) {
        this.bankMockRestClient = bankMockRestClient;
    }

    @Override
    public CardPaymentResponse charge(CardPaymentRequest request, String idempotencyKey) {
        return bankMockRestClient.post()
                .uri("/api/v1/payments/cards")
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .retrieve()
                .body(CardPaymentResponse.class);
    }

    @Override
    public CardPaymentResponse capture(String gatewayPaymentId, BigDecimal amount) {
        return bankMockRestClient.post()
                .uri("/api/v1/payments/cards/{id}/capture", gatewayPaymentId)
                .body(new CaptureRequest(amount))
                .retrieve()
                .body(CardPaymentResponse.class);
    }

    @Override
    public CardPaymentResponse refund(String gatewayPaymentId, BigDecimal amount) {
        return bankMockRestClient.post()
                .uri("/api/v1/payments/cards/{id}/refund", gatewayPaymentId)
                .body(new RefundRequest(amount, null))
                .retrieve()
                .body(CardPaymentResponse.class);
    }

    @Override
    public CardPaymentResponse voidPayment(String gatewayPaymentId) {
        return bankMockRestClient.post()
                .uri("/api/v1/payments/cards/{id}/void", gatewayPaymentId)
                .retrieve()
                .body(CardPaymentResponse.class);
    }

    @Override
    public CardPaymentResponse confirm3ds(String gatewayPaymentId, String otp) {
        return bankMockRestClient.post()
                .uri("/api/v1/payments/cards/{id}/3ds", gatewayPaymentId)
                .body(new ThreeDSConfirmRequest(otp))
                .retrieve()
                .body(CardPaymentResponse.class);
    }
}
