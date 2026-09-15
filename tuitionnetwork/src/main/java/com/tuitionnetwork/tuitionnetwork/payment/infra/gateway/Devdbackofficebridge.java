package com.tuitionnetwork.tuitionnetwork.payment.infra.gateway;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.Backofficegatewayport;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort.Outcome;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort.Result;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.BackofficePaymentPort;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.CustomerLookupPort;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.Amount;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BackofficePaymentRequest;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BackofficePaymentResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.BankMockApiException;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CustomerResponse;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.PaymentSource;
@Component
public class Devdbackofficebridge implements Backofficegatewayport{

    private static final Logger log = LoggerFactory.getLogger(Devdbackofficebridge.class);

    private final BackofficePaymentPort payments;
    private final CustomerLookupPort customers;

    public Devdbackofficebridge(BackofficePaymentPort payments, CustomerLookupPort customers) {
        this.payments = payments;
        this.customers = customers;
    }
    @Override
    public Optional<CustomerProfile> lookupCustomer(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            return Optional.empty();
        }

        try {
            CustomerResponse response = customers.findByNationalId(nationalId);
            if (response == null || response.customerId() == null) {
                return Optional.empty();
            }
            return Optional.of(toProfile(response));

        } catch (BankMockApiException e) {
            log.debug("Customer lookup returned {}", e.code());
            return Optional.empty();

        } catch (Exception e) {
            log.error("Customer lookup failed", e);
            return Optional.empty();
        }
    }

    private CustomerProfile toProfile(CustomerResponse response) {
        List<Account> accounts = response.accounts() == null ? List.of()
                : response.accounts().stream()
                        .map(account -> new Account(
                                account.accountId(), account.accountNumber(), account.type(),
                                account.currency(), account.availableBalance(), account.status()))
                        .toList();

        List<Card> cards = response.cards() == null ? List.of()
                : response.cards().stream()
                        .map(card -> new Card(
                                card.cardId(), card.maskedNumber(), card.scheme(), card.type(),
                                card.holderName(), card.expiry(), card.status(),
                                card.availableLimit(), card.linkedAccountId()))
                        .toList();

        return new CustomerProfile(
                response.customerId(), response.fullNameEnglish(), response.status(),
                accounts, cards);
    }
    @Override
    public Result chargeSource(SourceChargeCommand command, String idempotencyKey) {
        return call(null, () -> payments.charge(
                new BackofficePaymentRequest(
                        command.sourceId(),
                        new Amount(command.amount(), command.currency()),
                        command.reference(),
                        command.description(),
                        command.capture()),
                idempotencyKey));
    }

    @Override
    public Result getSourcePaymentStatus(String gatewayPaymentId) {
        if (gatewayPaymentId == null || gatewayPaymentId.isBlank()) {
            return Result.unknown(null, "No provider reference to ask about");
        }
        return call(gatewayPaymentId, () -> payments.getStatus(gatewayPaymentId));
    }
    private Result call(String knownPaymentId, ProviderCall call) {
        try {
            return map(call.execute(), knownPaymentId);

        } catch (BankMockApiException e) {
            return mapFailure(e, knownPaymentId);

        } catch (RestClientException e) {
            log.error("Provider did not respond", e);
            return Result.unknown(knownPaymentId, "Provider did not respond");

        } catch (RuntimeException e) {
            log.error("Provider call failed", e);
            return Result.unknown(knownPaymentId, "Provider call failed");
        }
    }
    private Result map(BackofficePaymentResponse response, String knownPaymentId) {
        if (response == null) {
            return Result.unknown(knownPaymentId, "Provider returned an empty response");
        }

        String paymentId = response.paymentId() == null ? knownPaymentId : response.paymentId();
        String status = response.status() == null ? "" : response.status().toUpperCase();

        boolean approved = Boolean.TRUE.equals(response.approved())
                || "POSTED".equals(status)
                || "CAPTURED".equals(status)
                || "AUTHORISED".equals(status)
                || "AUTHORIZED".equals(status);

        if (!approved) {
            return new Result(Outcome.DECLINED, paymentId, response.status(),
                    null, null, null,
                    response.responseCode(), response.responseMessage(),
                    maskedNumber(response.source()), scheme(response.source()),
                    productType(response.source()), null);
        }

        PaymentSource source = response.source();
        return new Result(
                Outcome.APPROVED,
                paymentId,
                response.status(),
                response.capturedAmount() != null ? response.capturedAmount() : response.amount(),
                response.authCode(),
                response.rrn(),
                response.responseCode(),
                response.responseMessage(),
                maskedNumber(source),
                scheme(source),
                productType(source),
                null);
    }
    private Result mapFailure(BankMockApiException e, String knownPaymentId) {
        Map<String, Object> details = e.details() == null ? Map.of() : e.details();
        String paymentId = detail(details, "payment_id", knownPaymentId);
        String code = e.code() == null ? "" : e.code();
        if ("SOURCE_NOT_FOUND".equals(code)
                || "PAYMENT_NOT_FOUND".equals(code)
                || "CUSTOMER_NOT_FOUND".equals(code)
                || "HTTP_404".equals(code)) {
            return Result.notFound(paymentId);
        }

        if ("ISSUER_UNAVAILABLE".equals(code)
                || "HTTP_500".equals(code)
                || "HTTP_502".equals(code)
                || "HTTP_503".equals(code)
                || "HTTP_504".equals(code)) {
            log.error("Provider could not confirm the outcome: {}", code);
            return Result.unknown(paymentId, "Provider did not confirm the outcome");
        }
        Outcome outcome = ("INSUFFICIENT_FUNDS".equals(code) || code.startsWith("CARD_"))
                ? Outcome.DECLINED
                : Outcome.REJECTED;

        return new Result(outcome, paymentId, null, null, null, null,
                detail(details, "response_code", null), e.getMessage(),
                null, null, null, code);
    }

    private static String maskedNumber(PaymentSource source) {
        return source == null ? null : source.maskedNumber();
    }

    private static String scheme(PaymentSource source) {
        return source == null ? null : source.scheme();
    }

    private static String productType(PaymentSource source) {
        return source == null ? null : source.productType();
    }

    private static String detail(Map<String, Object> details, String key, String fallback) {
        Object value = details.get(key);
        return value == null ? fallback : value.toString();
    }

    @FunctionalInterface
    private interface ProviderCall {
        BackofficePaymentResponse execute();
    }
}