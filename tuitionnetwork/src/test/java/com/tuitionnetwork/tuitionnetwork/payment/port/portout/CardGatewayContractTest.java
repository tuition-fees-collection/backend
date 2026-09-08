package com.tuitionnetwork.tuitionnetwork.payment.port.portout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The gateway contract carries the single most dangerous decision in the module:
 * whether an outcome means "no money moved" or "we do not know". Collapsing those
 * two loses real payments, so they get a test.
 */
class CardGatewayContractTest {

    @Nested
    @DisplayName("outcome classification")
    class Outcomes {

        @ParameterizedTest
        @EnumSource(value = CardgatewayPort.Outcome.class, names = {"DECLINED", "REJECTED"})
        @DisplayName("a decline means no money moved, so FAILED is safe")
        void definiteFailures(CardgatewayPort.Outcome outcome) {
            assertTrue(outcome.isDefiniteFailure());
            assertFalse(outcome.needsReconciliation(), outcome + " is resolved, not ambiguous");
            assertFalse(outcome.isRetryable(), "retrying a decline just declines again");
        }

        @Test
        @DisplayName("an unavailable issuer must never be treated as a failure")
        void unknownIsNotAFailure() {
            var unknown = CardgatewayPort.Outcome.UNKNOWN;
            assertFalse(unknown.isDefiniteFailure(),
                    "marking UNKNOWN as FAILED destroys a payment that may have succeeded");
            assertTrue(unknown.isRetryable(), "safe to resend with the same Idempotency-Key");
            assertTrue(unknown.needsReconciliation());
        }

        @Test
        @DisplayName("NOT_FOUND is ambiguous, not reassuring")
        void notFoundNeedsAHuman() {
            var notFound = CardgatewayPort.Outcome.NOT_FOUND;
            assertFalse(notFound.isDefiniteFailure(),
                    "the mock loses everything on restart, so absence does not prove nothing happened");
            assertFalse(notFound.isRetryable(), "resending would charge again");
            assertTrue(notFound.needsReconciliation());
        }

        @ParameterizedTest
        @EnumSource(value = CardgatewayPort.Outcome.class, names = {"APPROVED", "REQUIRES_3DS"})
        @DisplayName("a successful or in-progress outcome is neither a failure nor a retry")
        void successPathIsClean(CardgatewayPort.Outcome outcome) {
            assertFalse(outcome.isDefiniteFailure());
            assertFalse(outcome.isRetryable());
            assertFalse(outcome.needsReconciliation());
        }

        @Test
        @DisplayName("no outcome is both a definite failure and unresolved")
        void classificationsAreExclusive() {
            for (var outcome : CardgatewayPort.Outcome.values()) {
                assertFalse(outcome.isDefiniteFailure() && outcome.needsReconciliation(),
                        outcome + " cannot be both settled and ambiguous");
            }
        }
    }

    @Nested
    @DisplayName("ChargeCommand")
    class Charge {

        @Test
        @DisplayName("toString hides the PAN and the CVV")
        void isRedacted() {
            var command = new CardgatewayPort.ChargeCommand(UUID.randomUUID(),
                    "4111111111111111", "MOHAMED HAMOUDA", 12, 2030, "123",
                    new BigDecimal("10000.00"), "EGP", "Tuition", true);

            String printed = command.toString();
            assertFalse(printed.contains("4111111111111111"));
            assertFalse(printed.contains("111111"));
            assertFalse(printed.contains("123"));
            assertTrue(printed.contains("REDACTED"));
        }

        @Test
        @DisplayName("a charge of zero or less is rejected before it reaches the provider")
        void rejectsNonPositiveAmount() {
            assertThrows(IllegalArgumentException.class, () -> charge(BigDecimal.ZERO));
            assertThrows(IllegalArgumentException.class, () -> charge(new BigDecimal("-1.00")));
            assertThrows(IllegalArgumentException.class, () -> charge(null));
        }

        private CardgatewayPort.ChargeCommand charge(BigDecimal amount) {
            return new CardgatewayPort.ChargeCommand(UUID.randomUUID(), "4111111111111111",
                    "X", 12, 2030, "123", amount, "EGP", null, true);
        }
    }

    @Nested
    @DisplayName("Result")
    class Results {

        @Test
        @DisplayName("the unknown factory produces a result that cannot be mistaken for a failure")
        void unknownFactory() {
            var result = CardgatewayPort.Result.unknown("pay_1", "ISSUER_UNAVAILABLE");
            assertEquals(CardgatewayPort.Outcome.UNKNOWN, result.outcome());
            assertFalse(result.isApproved());
            assertFalse(result.outcome().isDefiniteFailure());
            assertEquals("pay_1", result.gatewayPaymentId());
        }

        @Test
        @DisplayName("the not-found factory keeps the id so a human can chase it")
        void notFoundFactory() {
            var result = CardgatewayPort.Result.notFound("pay_2");
            assertEquals(CardgatewayPort.Outcome.NOT_FOUND, result.outcome());
            assertEquals("pay_2", result.gatewayPaymentId());
        }

        @Test
        @DisplayName("EPP eligibility follows the card type, and debit is never eligible")
        void creditOnly() {
            assertTrue(result("CREDIT").isCreditCard());
            assertTrue(result("credit").isCreditCard(), "the provider's casing must not matter");
            assertFalse(result("DEBIT").isCreditCard());
            assertFalse(result(null).isCreditCard(), "an absent card type is not a credit card");
        }

        private CardgatewayPort.Result result(String cardType) {
            return new CardgatewayPort.Result(CardgatewayPort.Outcome.APPROVED, "pay_1", "CAPTURED",
                    new BigDecimal("100.00"), "195000", "3355", "00", "Approved",
                    "411111******1111", "VISA", cardType, null);
        }
    }

    @Nested
    @DisplayName("BillingPort.PayableFee")
    class Fees {

        @Test
        @DisplayName("outstanding is what is still owed, not the original amount")
        void outstandingSubtractsWhatWasPaid() {
            var fee = fee("18000.00", "8000.00", "PARTIALLY_PAID");
            assertEquals(0, new BigDecimal("10000.00").compareTo(fee.outstanding()));
        }

        @Test
        @DisplayName("a settled fee is not payable")
        void settledIsNotPayable() {
            assertTrue(fee("100.00", "0.00", "OUTSTANDING").isPayable());
            assertTrue(fee("100.00", "40.00", "PARTIALLY_PAID").isPayable());
            assertFalse(fee("100.00", "100.00", "SETTLED").isPayable(),
                    "paying an already settled fee must not be possible - SYS-US-02");
        }

        private BillingPort.PayableFee fee(String amount, String paid, String status) {
            return new BillingPort.PayableFee(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "TUITION", "2026-T1", new BigDecimal(amount), new BigDecimal(paid), status, 1);
        }
    }
}