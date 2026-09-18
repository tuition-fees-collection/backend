package com.tuitionnetwork.tuitionnetwork.payment.infra.gateway;

import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.Amount;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BankMockApiException;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardDetails;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardPaymentResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
@Primary
public class DevDCardGatewayBridge implements CardGatewayPort {

    private final com.tuitionnetwork.tuitionnetwork.payments.infra.ports.CardGatewayPort delegate;

    public DevDCardGatewayBridge(
            com.tuitionnetwork.tuitionnetwork.payments.infra.ports.CardGatewayPort delegate) {
        this.delegate = delegate;
    }

    @Override
    public Result charge(ChargeCommand command, String idempotencyKey) {
        CardPaymentRequest request = new CardPaymentRequest(
                new CardDetails(command.cardNumber(), command.cardHolderName(),
                        command.expiryMonth(), command.expiryYear(), command.cvv()),
                new Amount(command.amount(), command.currency()),
                command.paymentId().toString(), null, command.description(), command.capture());
        return call(null, () -> delegate.charge(request, idempotencyKey));
    }

    @Override
    public Result confirm3ds(String gatewayPaymentId, String otp) {
        return call(gatewayPaymentId, () -> delegate.confirm3ds(gatewayPaymentId, otp));
    }

    @Override
    public Result getStatus(String gatewayPaymentId) {
        return call(gatewayPaymentId, () -> delegate.getStatus(gatewayPaymentId));
    }

    private Result call(String knownPaymentId, GatewayCall call) {
        try {
            return map(call.execute(), knownPaymentId);
        } catch (BankMockApiException exception) {
            return mapFailure(exception, knownPaymentId);
        } catch (RestClientException exception) {
            return Result.unknown(knownPaymentId, "Provider did not respond");
        } catch (RuntimeException exception) {
            return Result.unknown(knownPaymentId, "Provider call failed");
        }
    }

    private Result map(CardPaymentResponse response, String knownPaymentId) {
        if (response == null) {
            return Result.unknown(knownPaymentId, "Provider returned an empty response");
        }
        String paymentId = response.paymentId() == null ? knownPaymentId : response.paymentId();
        String status = response.status() == null ? "" : response.status().toUpperCase();
        Outcome outcome;
        if ("PENDING_3DS".equals(status)) {
            outcome = Outcome.REQUIRES_3DS;
        } else if (Boolean.TRUE.equals(response.approved())
                || "CAPTURED".equals(status)
                || "AUTHORISED".equals(status)
                || "AUTHORIZED".equals(status)) {
            outcome = Outcome.APPROVED;
        } else {
            outcome = Outcome.DECLINED;
        }

        return new Result(outcome, paymentId, response.status(), response.capturedAmount(),
                response.authCode(), response.rrn(), response.responseCode(), response.responseMessage(),
                response.card() == null ? null : response.card().maskedNumber(),
                response.card() == null ? null : response.card().scheme(),
                response.card() == null ? null : response.card().type(), null,
                null,
                null,
                response.orderReference(),
                response.amount());
    }

    private Result mapFailure(BankMockApiException exception, String knownPaymentId) {
        Map<String, Object> details = exception.details() == null ? Map.of() : exception.details();
        String paymentId = detail(details, "payment_id", knownPaymentId);
         if ("PAYMENT_NOT_FOUND".equals(exception.code())
                || "HTTP_404".equals(exception.code())) {
            return Result.notFound(paymentId);
        }
        if ("ISSUER_UNAVAILABLE".equals(exception.code())
                || "HTTP_500".equals(exception.code())
                || "HTTP_502".equals(exception.code())
                || "HTTP_503".equals(exception.code())
                || "HTTP_504".equals(exception.code())) {
            return Result.unknown(paymentId, "Provider did not confirm the outcome");
        }
        Outcome outcome = exception.code().startsWith("CARD_")
                ? Outcome.DECLINED : Outcome.REJECTED;
        return new Result(outcome, paymentId, null, null, null, null,
                detail(details, "response_code", null), exception.getMessage(),
                null, null, null, exception.code(),
                null, null,
                detail(details, "order_reference", null),
                null);
    }

    private String detail(Map<String, Object> details, String key, String fallback) {
        Object value = details.get(key);
        return value == null ? fallback : value.toString();
    }

    @FunctionalInterface
    private interface GatewayCall {
        CardPaymentResponse execute();
    }
}