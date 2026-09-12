package com.tuitionnetwork.tuitionnetwork.payment.infra.gateway;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
@Component
public class WitCardGatewayAdapter implements CardGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(WitCardGatewayAdapter.class);

    private static final String CARDS_PATH = "/api/v1/payments/cards";

    private final RestClient client;

    public WitCardGatewayAdapter(
            @Value("${wit.gateway.base-url:http://localhost:8000}") String baseUrl,
            @Value("${wit.gateway.api-key:wit-intern-2026}") String apiKey,
            @Value("${wit.gateway.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${wit.gateway.read-timeout-ms:15000}") int readTimeoutMs) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    @Override
    public Result charge(ChargeCommand command, String idempotencyKey) {
        CardRequest body = CardRequest.from(command);

        return call("charge", null, () -> client.post()
                .uri(CARDS_PATH)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .toEntity(CardResponse.class));
    }

    @Override
    public Result confirm3ds(String gatewayPaymentId, String otp) {
        return call("confirm3ds", gatewayPaymentId, () -> client.post()
                .uri(CARDS_PATH + "/{id}/3ds", gatewayPaymentId)
                .body(Map.of("otp", otp))
                .retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .toEntity(CardResponse.class));
    }

    @Override
    public Result getStatus(String gatewayPaymentId) {
        return call("getStatus", gatewayPaymentId, () -> client.get()
                .uri(CARDS_PATH + "/{id}", gatewayPaymentId)
                .retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .toEntity(CardResponse.class));
    }
    private Result call(String operation, String knownPaymentId, GatewayCall call) {
        try {
            ResponseEntity<CardResponse> response = call.execute();
            return toResult(response.getStatusCode(), response.getBody(), knownPaymentId);

        } catch (ResourceAccessException e) {
            log.warn("Gateway {} timed out or was unreachable, outcome unknown: {}",
                    operation, e.getMessage());
            return Result.unknown(knownPaymentId, "Provider did not respond");

        } catch (RuntimeException e) {
            log.error("Gateway {} failed unexpectedly, outcome unknown", operation, e);
            return Result.unknown(knownPaymentId, "Provider call failed");
        }
    }
    private Result toResult(HttpStatusCode status, CardResponse body, String knownPaymentId) {
        String paymentId = body != null && body.anyPaymentId() != null
                ? body.anyPaymentId()
                : knownPaymentId;

        if (status.is5xxServerError()) {
            return Result.unknown(paymentId, message(body, "Provider unavailable"));
        }
        if (status.value() == 404) {
            return Result.notFound(paymentId);
        }
        if (status.is2xxSuccessful() && body != null) {
            return fromBody(body, paymentId);
        }
        if (status.value() == 402) {
            return failure(Outcome.DECLINED, body, paymentId, "Card declined");
        }
        if (status.is4xxClientError()) {
            return failure(Outcome.REJECTED, body, paymentId, "Request rejected by provider");
        }
        return Result.unknown(paymentId, "Unreadable provider response");
    }
    private Result fromBody(CardResponse body, String paymentId) {
        String gatewayStatus = body.status() == null ? "" : body.status().toUpperCase();

        Outcome outcome;
        if ("PENDING_3DS".equals(gatewayStatus)) {
            outcome = Outcome.REQUIRES_3DS;
        } else if (Boolean.TRUE.equals(body.approved())
                || "CAPTURED".equals(gatewayStatus)
                || "AUTHORISED".equals(gatewayStatus)
                || "AUTHORIZED".equals(gatewayStatus)) {
            outcome = Outcome.APPROVED;
        } else {
            outcome = Outcome.DECLINED;
        }

        CardDetails card = body.card();
        return new Result(
                outcome,
                paymentId,
                body.status(),
                body.capturedAmount(),
                body.authCode(),
                body.rrn(),
                body.anyResponseCode(),
                body.anyMessage(),
                card == null ? null : card.maskedNumber(),
                card == null ? null : card.scheme(),
                card == null ? null : card.type(),
                body.errorCode());
    }

    private Result failure(Outcome outcome, CardResponse body, String paymentId, String fallback) {
        return new Result(
                outcome,
                paymentId,
                body == null ? null : body.status(),
                null,
                null,
                null,
                body == null ? null : body.anyResponseCode(),
                message(body, fallback),
                null,
                null,
                null,
                body == null ? null : body.errorCode());
    }

    private String message(CardResponse body, String fallback) {
        if (body == null) {
            return fallback;
        }
        return body.anyMessage() != null ? body.anyMessage() : fallback;
    }

    @FunctionalInterface
    private interface GatewayCall {
        ResponseEntity<CardResponse> execute();
    }
    private record CardRequest(
            Card card,
            Amount amount,
            @JsonProperty("order_reference") String orderReference,
            String description,
            boolean capture) {
        static CardRequest from(ChargeCommand c) {
            return new CardRequest(
                    new Card(c.cardNumber(), c.cardHolderName(), c.expiryMonth(), c.expiryYear(), c.cvv()),
                    new Amount(c.amount(), c.currency()),
                    c.paymentId().toString(),
                    c.description(),
                    c.capture());
        }
    }

    private record Card(
            String number,
            @JsonProperty("holder_name") String holderName,
            @JsonProperty("expiry_month") int expiryMonth,
            @JsonProperty("expiry_year") int expiryYear,
            String cvv) {
    }

    private record Amount(BigDecimal value, String currency) {
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CardResponse(
            @JsonProperty("payment_id") String paymentId,
            String status,
            Boolean approved,
            @JsonProperty("captured_amount") BigDecimal capturedAmount,
            CardDetails card,
            @JsonProperty("auth_code") String authCode,
            String rrn,
            @JsonProperty("response_code") String responseCode,
            @JsonProperty("response_message") String responseMessage,
            ErrorBody error,
            @JsonProperty("request_id") String requestId) {
        String errorCode() {
            return error == null ? null : error.code();
        }
        String anyPaymentId() {
            if (paymentId != null) {
                return paymentId;
            }
            return error == null ? null : error.detail("payment_id");
        }
        String anyResponseCode() {
            if (responseCode != null) {
                return responseCode;
            }
            return error == null ? null : error.detail("response_code");
        }

        String anyMessage() {
            if (responseMessage != null) {
                return responseMessage;
            }
            return error == null ? null : error.message();
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ErrorBody(String code, String message, Map<String, Object> details) {

        String detail(String key) {
            if (details == null) {
                return null;
            }
            Object value = details.get(key);
            return value == null ? null : value.toString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CardDetails(
            @JsonProperty("masked_number") String maskedNumber,
            String scheme,
            String type) {
    }
}