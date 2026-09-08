package com.tuitionnetwork.tuitionnetwork.payment.domain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Unit tests for the Payment aggregate. No Spring, no database - the whole point
 * of keeping the rules in the domain object is that they are testable like this.
 */
class PaymentTest {

    private static final UUID GUARDIAN = UUID.randomUUID();
    private static final UUID OFFICER = UUID.randomUUID();

    private static Payment order(PaymentMethod method, String amount) {
        return Payment.create("idem-" + UUID.randomUUID(), GUARDIAN, OFFICER,
                method, PaymentType.FULL, new BigDecimal(amount), "EGP");
    }

    private static Payment pendingOrder(PaymentMethod method) {
        Payment p = order(method, "1000.00");
        p.markPending();
        return p;
    }

    private static void approve(Payment p) {
        p.markSuccessful("pay_" + UUID.randomUUID(), p.getAmount(), "195000",
                "335531777886", "00", "Approved", "411111******1111", "VISA");
    }

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("starts in CREATED so the NOT NULL status column is never sent a null")
        void startsCreated() {
            assertEquals(PaymentStatus.CREATED, order(PaymentMethod.CASH, "100.00").getStatus());
        }

        @Test
        @DisplayName("rejects a null actor because processed_by is NOT NULL in the schema")
        void rejectsNullActor() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> Payment.create("k", GUARDIAN, null, PaymentMethod.CASH,
                            PaymentType.FULL, new BigDecimal("100.00"), "EGP"));
            assertTrue(e.getMessage().contains("processedBy"));
        }

        @Test
        @DisplayName("rejects a zero or negative amount")
        void rejectsNonPositiveAmount() {
            assertThrows(IllegalArgumentException.class, () -> order(PaymentMethod.CASH, "0.00"));
            assertThrows(IllegalArgumentException.class, () -> order(PaymentMethod.CASH, "-5.00"));
        }

        @Test
        @DisplayName("rejects more than 2 decimal places - the column is NUMERIC(12,2)")
        void rejectsTooManyDecimals() {
            assertThrows(IllegalArgumentException.class, () -> order(PaymentMethod.CASH, "10.005"));
        }

        @Test
        @DisplayName("rejects a blank idempotency key")
        void rejectsBlankKey() {
            assertThrows(IllegalArgumentException.class,
                    () -> Payment.create("  ", GUARDIAN, OFFICER, PaymentMethod.CASH,
                            PaymentType.FULL, new BigDecimal("100.00"), "EGP"));
        }

        @Test
        @DisplayName("defaults the currency to EGP")
        void defaultsCurrency() {
            Payment p = Payment.create("k", GUARDIAN, OFFICER, PaymentMethod.CASH,
                    PaymentType.FULL, new BigDecimal("100.00"), null);
            assertEquals("EGP", p.getCurrency());
        }
    }

    @Nested
    @DisplayName("state machine")
    class StateMachine {

        @Test
        @DisplayName("the happy path reaches SUCCESSFUL and stamps settled_at")
        void happyPath() {
            Payment p = pendingOrder(PaymentMethod.CREDIT_CARD);
            approve(p);
            assertEquals(PaymentStatus.SUCCESSFUL, p.getStatus());
            assertNotNull(p.getSettledAt());
        }

        @Test
        @DisplayName("a FAILED payment can never become SUCCESSFUL")
        void failedIsTerminal() {
            Payment p = pendingOrder(PaymentMethod.CREDIT_CARD);
            p.markFailed("CARD_DECLINED", "51");
            IllegalPaymentTransitionException e =
                    assertThrows(IllegalPaymentTransitionException.class, () -> approve(p));
            assertEquals(PaymentStatus.FAILED, e.getFrom());
            assertEquals(PaymentStatus.SUCCESSFUL, e.getTo());
        }

        @Test
        @DisplayName("a SUCCESSFUL payment cannot be failed or re-approved")
        void successfulIsTerminal() {
            Payment p = pendingOrder(PaymentMethod.CREDIT_CARD);
            approve(p);
            assertThrows(IllegalPaymentTransitionException.class, () -> p.markFailed("late", "05"));
            assertThrows(IllegalPaymentTransitionException.class, () -> approve(p));
        }

        @Test
        @DisplayName("CREATED cannot jump straight to SUCCESSFUL without an attempt")
        void noJumpFromCreated() {
            Payment p = order(PaymentMethod.CREDIT_CARD, "100.00");
            assertThrows(IllegalPaymentTransitionException.class, () -> approve(p));
        }

        @Test
        @DisplayName("an unknown gateway outcome parks the payment in RECON_REQUIRED")
        void unknownOutcomeIsFlagged() {
            Payment p = pendingOrder(PaymentMethod.CREDIT_CARD);
            p.flagForReconciliation("ISSUER_UNAVAILABLE, outcome unknown");
            assertEquals(PaymentStatus.RECON_REQUIRED, p.getStatus());
        }

        @Test
        @DisplayName("reconciliation is the only route from a non-success state to SUCCESSFUL")
        void reconciliationResolvesEitherWay() {
            Payment won = pendingOrder(PaymentMethod.CREDIT_CARD);
            won.flagForReconciliation("unknown");
            won.resolveReconciliation(PaymentStatus.SUCCESSFUL, "pay_x");
            assertEquals(PaymentStatus.SUCCESSFUL, won.getStatus());

            Payment lost = pendingOrder(PaymentMethod.CREDIT_CARD);
            lost.flagForReconciliation("unknown");
            lost.resolveReconciliation(PaymentStatus.FAILED, null);
            assertEquals(PaymentStatus.FAILED, lost.getStatus());
        }

        @Test
        @DisplayName("only a flagged payment can be reconciled")
        void cannotReconcileUnflagged() {
            Payment p = pendingOrder(PaymentMethod.CREDIT_CARD);
            assertThrows(IllegalStateException.class,
                    () -> p.resolveReconciliation(PaymentStatus.SUCCESSFUL, "pay_x"));
        }

        @Test
        @DisplayName("reconciliation cannot resolve to an in-flight state")
        void reconciliationResolvesToTerminalOnly() {
            Payment p = pendingOrder(PaymentMethod.CREDIT_CARD);
            p.flagForReconciliation("unknown");
            assertThrows(IllegalArgumentException.class,
                    () -> p.resolveReconciliation(PaymentStatus.PENDING, null));
        }

        @Test
        @DisplayName("cash settles without a gateway response")
        void cashSettles() {
            Payment p = pendingOrder(PaymentMethod.CASH);
            p.markSuccessfulCash();
            assertEquals(PaymentStatus.SUCCESSFUL, p.getStatus());
            assertEquals(0, p.getAmount().compareTo(p.getCapturedAmount()));
        }
    }

    @Nested
    @DisplayName("schema conformance")
    class SchemaConformance {

        @ParameterizedTest
        @EnumSource(PaymentStatus.class)
        @DisplayName("every status value fits status VARCHAR(20)")
        void statusFitsColumn(PaymentStatus status) {
            assertTrue(status.name().length() <= 20,
                    status + " is " + status.name().length() + " chars, column is VARCHAR(20)");
        }

        @ParameterizedTest
        @EnumSource(PaymentMethod.class)
        @DisplayName("every method value fits method VARCHAR(20)")
        void methodFitsColumn(PaymentMethod method) {
            assertTrue(method.name().length() <= 20);
        }

        @ParameterizedTest
        @EnumSource(PaymentType.class)
        @DisplayName("every type value fits payment_type VARCHAR(20)")
        void typeFitsColumn(PaymentType type) {
            assertTrue(type.name().length() <= 20);
        }

        @Test
        @DisplayName("a long provider message is truncated to response_message VARCHAR(255)")
        void truncatesResponseMessage() {
            Payment p = pendingOrder(PaymentMethod.CREDIT_CARD);
            p.markFailed("x".repeat(400), "05");
            assertEquals(255, p.getResponseMessage().length());
        }
    }

    @Nested
    @DisplayName("eligibility rules")
    class Eligibility {

        @Test
        @DisplayName("a receipt exists only for a confirmed payment - BO-US-11 AC1")
        void receiptOnlyAfterSuccess() {
            Payment p = order(PaymentMethod.CREDIT_CARD, "100.00");
            assertFalse(p.isReceiptEligible());
            p.markPending();
            assertFalse(p.isReceiptEligible());
            approve(p);
            assertTrue(p.isReceiptEligible());
        }

        @Test
        @DisplayName("EPP is offered on credit only - BO-US-09 AC1")
        void eppIsCreditOnly() {
            Payment credit = pendingOrder(PaymentMethod.CREDIT_CARD);
            approve(credit);
            assertTrue(credit.isEppEligible());

            Payment debit = pendingOrder(PaymentMethod.DEBIT_CARD);
            approve(debit);
            assertFalse(debit.isEppEligible());

            Payment cash = pendingOrder(PaymentMethod.CASH);
            cash.markSuccessfulCash();
            assertFalse(cash.isEppEligible());
        }

        @Test
        @DisplayName("EPP is not offered on a payment that has not succeeded")
        void eppNeedsSuccess() {
            Payment p = pendingOrder(PaymentMethod.CREDIT_CARD);
            assertFalse(p.isEppEligible());
            p.markFailed("declined", "51");
            assertFalse(p.isEppEligible());
        }
    }
}

