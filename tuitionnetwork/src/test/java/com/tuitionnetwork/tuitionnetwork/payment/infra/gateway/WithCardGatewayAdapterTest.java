package com.tuitionnetwork.tuitionnetwork.payment.infra.gateway;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort.ChargeCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort.Outcome;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort.Result;
class WitCardGatewayAdapterTest {
    private static final String BASE_URL = "http://127.0.0.1:8000";
    private static final String API_KEY = "wit-intern-2026";
    private final WitCardGatewayAdapter adapter =
            new WitCardGatewayAdapter(BASE_URL, API_KEY, 5000, 15000);
    @BeforeAll
    static void requireRunningMock() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 8000), 1000);
        } catch (IOException e) {
            Assumptions.abort("wit-mock-services is not running on 127.0.0.1:8000 - start it with ./run.sh");
        }
    }
    private Result charge(String cardNumber, String amount) {
        return adapter.charge(new ChargeCommand(
                UUID.randomUUID(),
                cardNumber,
                "Mohamed Hamouda",
                12,
                2030,
                "123",
                new BigDecimal(amount),
                "EGP",
                "Tuition payment",
                true), UUID.randomUUID().toString());
    }

    @Nested
    @DisplayName("approved")
    class Approved {
        @Test
        @DisplayName("a good credit card returns APPROVED with every gateway field populated")
        void approvesAndBindsEveryField() {
            Result result = charge("4111111111111111", "100.00");
            assertEquals(Outcome.APPROVED, result.outcome());
            assertEquals("CAPTURED", result.gatewayStatus());
            assertTrue(result.isApproved());
            assertNotNull(result.gatewayPaymentId(), "payment_id did not bind");
            assertTrue(result.gatewayPaymentId().startsWith("pay_"));
            assertNotNull(result.authCode(), "auth_code did not bind");
            assertNotNull(result.rrn(), "rrn did not bind");
            assertEquals("00", result.responseCode());
            assertEquals("Approved", result.responseMessage());
            assertEquals("411111******1111", result.maskedCardNumber());
            assertEquals("VISA", result.cardScheme());
            assertEquals("CREDIT", result.cardType());
            assertEquals(0, new BigDecimal("100.00").compareTo(result.capturedAmount()));
            assertNull(result.errorCode());
        }

        @Test
        @DisplayName("a debit card is approved but never EPP eligible - BO-US-09 AC1")
        void debitApprovesButIsNotEppEligible() {
            Result result = charge("4000056655665556", "100.00");

            assertEquals(Outcome.APPROVED, result.outcome());
            assertEquals("DEBIT", result.cardType());
            assertFalse(result.isCreditCard(), "EPP must not be offered on a debit card");
        }
    }

    @Nested
    @DisplayName("declined")
    class Declined {
        @Test
        @DisplayName("insufficient funds is DECLINED, carrying the issuer code and payment id")
        void insufficientFunds() {
            Result result = charge("4000000000000002", "100.00");

            assertEquals(Outcome.DECLINED, result.outcome());
            assertTrue(result.outcome().isDefiniteFailure(),
                    "a decline means no money moved, so FAILED is safe");
            assertFalse(result.outcome().isRetryable(), "retrying a decline just declines again");

            assertEquals("CARD_DECLINED", result.errorCode());
            assertEquals("51", result.responseCode(), "response_code lives in error.details");
            assertNotNull(result.responseMessage());
            assertNotNull(result.gatewayPaymentId(), "payment_id lives in error.details on a 402");
        }

        @Test
        @DisplayName("a malformed request is REJECTED, not DECLINED")
        void validationErrorIsRejected() {
            Result result = adapter.charge(new ChargeCommand(
                    UUID.randomUUID(), "4111111111111111", "M", 12, 2030, "123",
                    new BigDecimal("100.00"), "EGP", "Bad request", true),
                    UUID.randomUUID().toString());

            assertEquals(Outcome.REJECTED, result.outcome());
            assertTrue(result.outcome().isDefiniteFailure());
            assertEquals("VALIDATION_ERROR", result.errorCode());
        }
    }

    @Nested
    @DisplayName("3-D Secure")
    class ThreeDSecure {
        @Test
        @DisplayName("a 3DS card parks the payment, then the OTP completes it")
        void pendingThenConfirmed() {
            Result challenge = charge("4000000000003220", "100.00");

            assertEquals(Outcome.REQUIRES_3DS, challenge.outcome());
            assertEquals("PENDING_3DS", challenge.gatewayStatus());
            assertTrue(challenge.requires3ds());
            assertFalse(challenge.outcome().isDefiniteFailure(), "an OTP challenge is not a failure");
            assertNotNull(challenge.gatewayPaymentId());

            Result confirmed = adapter.confirm3ds(challenge.gatewayPaymentId(), "123456");

            assertEquals(Outcome.APPROVED, confirmed.outcome());
            assertEquals("CAPTURED", confirmed.gatewayStatus());
            assertNotNull(confirmed.authCode());
        }
    }

    @Nested
    @DisplayName("outcomes we must not guess at")
    class Unresolved {
        @Test
        @DisplayName("an unavailable issuer is UNKNOWN, never a failure")
        void issuerUnavailable() {
            Result result = charge("4000000000000119", "100.00");

            assertEquals(Outcome.UNKNOWN, result.outcome());
            assertFalse(result.outcome().isDefiniteFailure(),
                    "marking a 502 as FAILED loses a payment that may have succeeded");
            assertTrue(result.outcome().isRetryable(), "safe to resend with the same Idempotency-Key");
            assertTrue(result.outcome().needsReconciliation());
        }
        @Test
        @DisplayName("an unknown payment id is NOT_FOUND and needs a human, not an assumption")
        void statusOfUnknownPayment() {
            Result result = adapter.getStatus("pay_doesnotexist");

            assertEquals(Outcome.NOT_FOUND, result.outcome());
            assertFalse(result.outcome().isDefiniteFailure());
            assertFalse(result.outcome().isRetryable(), "resending would risk charging again");
            assertTrue(result.outcome().needsReconciliation());
        }

        @Test
        @DisplayName("an unreachable provider is UNKNOWN, not an exception the service must catch")
        void unreachableProvider() {
            WitCardGatewayAdapter offline =
                    new WitCardGatewayAdapter("http://127.0.0.1:9", API_KEY, 300, 300);

            Result result = offline.charge(new ChargeCommand(
                    UUID.randomUUID(), "4111111111111111", "Mohamed Hamouda", 12, 2030, "123",
                    new BigDecimal("100.00"), "EGP", "Offline", true),
                    UUID.randomUUID().toString());

            assertEquals(Outcome.UNKNOWN, result.outcome(),
                    "a connection failure means we do not know, not that it failed");
        }
    }

    @Nested
    @DisplayName("idempotency")
    class Idempotency {
        @Test
        @DisplayName("the same key twice returns the original payment, not a second charge")
        void sameKeyReturnsOriginalPayment() {
            String idempotencyKey = UUID.randomUUID().toString();
            ChargeCommand command = new ChargeCommand(
                    UUID.randomUUID(), "4111111111111111", "Mohamed Hamouda", 12, 2030, "123",
                    new BigDecimal("250.00"), "EGP", "Tuition payment", true);

            Result first = adapter.charge(command, idempotencyKey);
            Result second = adapter.charge(command, idempotencyKey);

            assertEquals(Outcome.APPROVED, first.outcome());
            assertEquals(Outcome.APPROVED, second.outcome());
            assertEquals(first.gatewayPaymentId(), second.gatewayPaymentId(),
                    "a replayed key must return the original payment, not create a new one");
        }
        @Test
        @DisplayName("different keys create different payments")
        void differentKeysCreateDifferentPayments() {
            Result first = charge("4111111111111111", "250.00");
            Result second = charge("4111111111111111", "250.00");

            assertNotNull(first.gatewayPaymentId());
            assertNotNull(second.gatewayPaymentId());
            assertFalse(first.gatewayPaymentId().equals(second.gatewayPaymentId()));
        }
    }
}