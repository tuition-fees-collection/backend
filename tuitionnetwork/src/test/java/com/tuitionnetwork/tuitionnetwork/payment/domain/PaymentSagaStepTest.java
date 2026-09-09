package com.tuitionnetwork.tuitionnetwork.payment.domain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PaymentSagaStepTest {

    private static final UUID PAYMENT = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Test
    @DisplayName("a planned step starts PENDING so the NOT NULL column is never sent a null")
    void startsPending() {
        assertEquals(PaymentSagaStepStatus.PENDING,
                PaymentSagaStep.plan(PAYMENT, PaymentSagaStepType.CHARGE).getStatus());
    }

    @Test
    @DisplayName("start() records the attempt time before the call goes out")
    void startStampsAttemptedAt() {
        PaymentSagaStep step = PaymentSagaStep.plan(PAYMENT, PaymentSagaStepType.CHARGE);
        step.start(NOW);
        assertEquals(PaymentSagaStepStatus.IN_PROGRESS, step.getStatus());
        assertNotNull(step.getAttemptedAt());
        assertNull(step.getCompletedAt());
    }

    @Test
    @DisplayName("a CHARGE left IN_PROGRESS is the unresolved case the status check exists for")
    void unresolvedCharge() {
        PaymentSagaStep charge = PaymentSagaStep.plan(PAYMENT, PaymentSagaStepType.CHARGE);
        charge.start(NOW);
        assertTrue(charge.isUnresolvedCharge());

        charge.complete(NOW);
        assertFalse(charge.isUnresolvedCharge(), "a finished charge is not unresolved");
    }

    @Test
    @DisplayName("a non-money step in progress is not an unresolved charge")
    void nonMoneyStepIsNotUnresolved() {
        PaymentSagaStep step = PaymentSagaStep.plan(PAYMENT, PaymentSagaStepType.VERIFY_ACCESS);
        step.start(NOW);
        assertFalse(step.isUnresolvedCharge());
    }

    @Test
    @DisplayName("completing clears any earlier error message")
    void completeClearsError() {
        PaymentSagaStep step = PaymentSagaStep.plan(PAYMENT, PaymentSagaStepType.SETTLE_FEES);
        step.start(NOW);
        step.complete(NOW);
        assertNull(step.getErrorMessage());
        assertNotNull(step.getCompletedAt());
    }

    @Test
    @DisplayName("a finished step cannot be finished twice")
    void finishedIsFinal() {
        PaymentSagaStep step = PaymentSagaStep.plan(PAYMENT, PaymentSagaStepType.CHARGE);
        step.start(NOW);
        step.complete(NOW);
        assertThrows(IllegalStateException.class, () -> step.complete(NOW));
        assertThrows(IllegalStateException.class, () -> step.fail("late", NOW));
        assertThrows(IllegalStateException.class, () -> step.start(NOW));
    }

    @Test
    @DisplayName("cash skips the charge step")
    void skipRecordsWhy() {
        PaymentSagaStep step = PaymentSagaStep.plan(PAYMENT, PaymentSagaStepType.CHARGE);
        step.skip("cash payment, no gateway", NOW);
        assertEquals(PaymentSagaStepStatus.SKIPPED, step.getStatus());
        assertEquals("cash payment, no gateway", step.getErrorMessage());
    }

    @Test
    @DisplayName("only CHARGE and CONFIRM_3DS move money")
    void moneyMovingSteps() {
        assertTrue(PaymentSagaStepType.CHARGE.isMoneyMoving());
        assertTrue(PaymentSagaStepType.CONFIRM_3DS.isMoneyMoving());
        assertFalse(PaymentSagaStepType.VERIFY_ACCESS.isMoneyMoving());
        assertFalse(PaymentSagaStepType.SETTLE_FEES.isMoneyMoving());
        assertFalse(PaymentSagaStepType.ISSUE_RECEIPT.isMoneyMoving());
    }

    @ParameterizedTest
    @EnumSource(PaymentSagaStepType.class)
    @DisplayName("every step name fits step_name VARCHAR(50)")
    void stepNameFitsColumn(PaymentSagaStepType type) {
        assertTrue(type.name().length() <= 50);
    }

    @ParameterizedTest
    @EnumSource(PaymentSagaStepStatus.class)
    @DisplayName("every step status fits status VARCHAR(20)")
    void stepStatusFitsColumn(PaymentSagaStepStatus status) {
        assertTrue(status.name().length() <= 20);
    }
}