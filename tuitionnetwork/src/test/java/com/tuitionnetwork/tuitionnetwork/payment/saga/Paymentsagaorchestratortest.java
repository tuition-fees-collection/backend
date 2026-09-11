package com.tuitionnetwork.tuitionnetwork.payment.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.Confirm3dsCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.ExecutePaymentCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.ExecutePaymentSagaUseCase.PaymentResultView;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.ReceiptPort;
import com.tuitionnetwork.tuitionnetwork.payment.services.PaymentAccessDeniedException;
import com.tuitionnetwork.tuitionnetwork.payment.services.strategy.CashHandler;
import com.tuitionnetwork.tuitionnetwork.payment.services.strategy.CreditCardHandler;
import com.tuitionnetwork.tuitionnetwork.payment.services.strategy.DebitCardHandler;
import com.tuitionnetwork.tuitionnetwork.payment.services.strategy.PaymentMethodHandler;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentSagaOrchestratorTest {

    private static final UUID GUARDIAN = UUID.randomUUID();
    private static final UUID OTHER_GUARDIAN = UUID.randomUUID();
    private static final UUID OFFICER = UUID.randomUUID();

    @Mock private PaymentRepository payments;
    @Mock private PaymentSettlement settlement;
    @Mock private PaymentSaga saga;
    @Mock private ReceiptPort receipts;
    @Mock private CardGatewayPort gateway;

    private PaymentSagaOrchestrator orchestrator;
    private Payment payment;

    @BeforeEach
    void setUp() {
        List<PaymentMethodHandler> handlers = List.of(
                new CashHandler(),
                new CreditCardHandler(gateway),
                new DebitCardHandler(gateway));

        orchestrator = new PaymentSagaOrchestrator(payments, settlement, saga, receipts, gateway, handlers);

        payment = persisted(PaymentMethod.CREDIT_CARD);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(settlement.markPending(any(Payment.class))).thenAnswer(call -> {
            Payment p = call.getArgument(0);
            p.markPending();
            return p;
        });

        when(saga.begin(any(), any())).thenReturn(null);
        when(receipts.findByPaymentId(any())).thenReturn(Optional.empty());
    }
    @Nested
    @DisplayName("an unknown outcome")
    class Unknown {
        @Test
        @DisplayName("parks the payment for reconciliation instead of failing it")
        void becomesReconRequired() {
            gatewayReturns(result(CardGatewayPort.Outcome.UNKNOWN, null, "Provider did not respond"));
            expectFlagged();

            PaymentResultView view = orchestrator.execute(chargeCommand());

            assertEquals(PaymentStatus.RECON_REQUIRED, view.status());
            assertTrue(view.isPending(), "an unknown outcome must be reported as pending");
            verify(settlement).flagForReconciliation(any(), any());
        }
        @Test
        @DisplayName("never settles the fee")
        void doesNotSettle() {
            gatewayReturns(result(CardGatewayPort.Outcome.UNKNOWN, null, "timeout"));
            expectFlagged();

            orchestrator.execute(chargeCommand());

            verify(settlement, never()).settleSuccess(any(), any());
            verify(settlement, never()).settleFailure(any(), anyString(), any());
        }

        @Test
        @DisplayName("leaves the charge step unresolved so the status check can find it")
        void leavesStepInProgress() {
            gatewayReturns(result(CardGatewayPort.Outcome.UNKNOWN, null, "timeout"));
            expectFlagged();

            orchestrator.execute(chargeCommand());

            verify(saga).leaveUnresolved(any(), anyString());
            verify(saga, never()).succeed(any());
            verify(saga, never()).fail(any(), anyString());
        }
        @Test
        @DisplayName("a provider with no record is treated the same way")
        void notFoundAlsoParks() {
            gatewayReturns(result(CardGatewayPort.Outcome.NOT_FOUND, null, "No record at provider"));
            expectFlagged();

            PaymentResultView view = orchestrator.execute(chargeCommand());

            assertEquals(PaymentStatus.RECON_REQUIRED, view.status());
            verify(settlement, never()).settleFailure(any(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("an approved charge")
    class Approved {

        @Test
        @DisplayName("settles through the full path, not just a status change")
        void settles() {
            gatewayReturns(result(CardGatewayPort.Outcome.APPROVED, "CAPTURED", "Approved"));
            when(settlement.settleSuccess(any(), any())).thenAnswer(call -> {
                Payment p = call.getArgument(0);
                p.markSuccessful("pay_1", p.getAmount(), "195000", "3355", "00",
                        "Approved", "411111******1111", "VISA");
                return p;
            });
            when(receipts.findByPaymentId(any())).thenReturn(
                    Optional.of(new ReceiptPort.IssuedReceipt(
                            UUID.randomUUID(), payment.getId(), "RCP-2026-TESTTEST")));

            PaymentResultView view = orchestrator.execute(chargeCommand());

            assertEquals(PaymentStatus.SUCCESSFUL, view.status());
            assertFalse(view.isPending());
            assertEquals("RCP-2026-TESTTEST", view.receiptReference());
            verify(settlement).settleSuccess(any(), any());
            verify(saga).succeed(any());
        }
    }

    @Nested
    @DisplayName("a declined card")
    class Declined {

        @Test
        @DisplayName("fails the payment without touching the fee")
        void failsCleanly() {
            gatewayReturns(declined());
            when(settlement.settleFailure(any(), anyString(), any())).thenAnswer(call -> {
                Payment p = call.getArgument(0);
                p.markFailed(call.getArgument(1), "51");
                return p;
            });

            PaymentResultView view = orchestrator.execute(chargeCommand());

            assertEquals(PaymentStatus.FAILED, view.status());
            assertNull(view.receiptReference(), "a failed payment has no receipt");
            verify(settlement, never()).settleSuccess(any(), any());
            verify(saga).fail(any(), anyString());
        }
        @Test
        @DisplayName("shows our wording, not the provider's raw message")
        void safeMessage() {
            gatewayReturns(declined());
            when(settlement.settleFailure(any(), anyString(), any())).thenAnswer(call -> {
                Payment p = call.getArgument(0);
                p.markFailed(call.getArgument(1), "51");
                return p;
            });

            PaymentResultView view = orchestrator.execute(chargeCommand());

            assertTrue(view.message().contains("declined"));
            assertFalse(view.message().contains("Insufficient funds"),
                    "the provider's raw text must not reach the guardian");
        }
    }

    @Nested
    @DisplayName("3-D Secure")
    class ThreeDSecure {

        @Test
        @DisplayName("leaves the payment pending and asks for an OTP")
        void parksForOtp() {
            gatewayReturns(result(CardGatewayPort.Outcome.REQUIRES_3DS, "PENDING_3DS", null));
            when(settlement.attachGatewayId(any(), anyString())).thenAnswer(call -> call.getArgument(0));

            PaymentResultView view = orchestrator.execute(chargeCommand());

            assertTrue(view.requires3ds());
            assertEquals(PaymentStatus.PENDING, view.status());
            assertTrue(view.isPending());
            verify(settlement, never()).settleSuccess(any(), any());
            verify(settlement, never()).settleFailure(any(), anyString(), any());
        }

        @Test
        @DisplayName("cannot be confirmed on a payment with no provider reference")
        void needsAReference() {
            assertThrows(IllegalStateException.class,
                    () -> orchestrator.confirm3ds(new Confirm3dsCommand(payment.getId(), GUARDIAN, "123456")));
        }
    }

    @Nested
    @DisplayName("cash")
    class Cash {

        @Test
        @DisplayName("settles without contacting the provider at all")
        void neverCallsTheGateway() {
            payment = persisted(PaymentMethod.CASH);
            when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
            when(settlement.settleSuccess(any(), any())).thenAnswer(call -> {
                Payment p = call.getArgument(0);
                p.markSuccessfulCash();
                return p;
            });

            PaymentResultView view = orchestrator.execute(
                    new ExecutePaymentCommand(payment.getId(), GUARDIAN, null, null, 0, 0, null));

            assertEquals(PaymentStatus.SUCCESSFUL, view.status());
            verify(gateway, never()).charge(any(), anyString());
        }
    }

    @Nested
    @DisplayName("wrong route")
    class WrongRoute {
        @Test
        @DisplayName("a cash payment sent to /pay with a card is refused")
        void cashWithCard() {
            payment = persisted(PaymentMethod.CASH);
            when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> orchestrator.execute(chargeCommand()));
            assertTrue(thrown.getMessage().contains("pay-cash"));
            verify(gateway, never()).charge(any(), anyString());
        }

        @Test
        @DisplayName("a card payment sent to /pay-cash is refused")
        void cardWithoutCard() {
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> orchestrator.execute(
                            new ExecutePaymentCommand(payment.getId(), GUARDIAN, null, null, 0, 0, null)));
            assertTrue(thrown.getMessage().contains("card details"));
        }
    }

    @Nested
    @DisplayName("ownership")
    class Ownership {

        @Test
        @DisplayName("another guardian cannot charge this payment")
        void wrongGuardian() {
            assertThrows(PaymentAccessDeniedException.class,
                    () -> orchestrator.execute(new ExecutePaymentCommand(
                            payment.getId(), OTHER_GUARDIAN,
                            "4111111111111111", "Mohamed", 12, 2030, "123")));

            verify(gateway, never()).charge(any(), anyString());
        }

        @Test
        @DisplayName("an unknown payment id looks the same as someone else's")
        void unknownPayment() {
            UUID missing = UUID.randomUUID();
            when(payments.findById(missing)).thenReturn(Optional.empty());

            assertThrows(PaymentAccessDeniedException.class,
                    () -> orchestrator.execute(new ExecutePaymentCommand(
                            missing, GUARDIAN, "4111111111111111", "Mohamed", 12, 2030, "123")));
        }
    }
    private void gatewayReturns(CardGatewayPort.Result result) {
        when(gateway.charge(any(), anyString())).thenReturn(result);
        when(gateway.confirm3ds(anyString(), anyString())).thenReturn(result);
    }

    private void expectFlagged() {
        when(settlement.flagForReconciliation(any(), any())).thenAnswer(call -> {
            Payment p = call.getArgument(0);
            p.flagForReconciliation("unknown");
            return p;
        });
    }

    private ExecutePaymentCommand chargeCommand() {
        return new ExecutePaymentCommand(payment.getId(), GUARDIAN,
                "4111111111111111", "Mohamed Hamouda", 12, 2030, "123");
    }

    private static CardGatewayPort.Result result(CardGatewayPort.Outcome outcome,
                                                 String gatewayStatus, String message) {
        return new CardGatewayPort.Result(outcome, "pay_test", gatewayStatus,
                new BigDecimal("500.00"), "195000", "3355", "00", message,
                "411111******1111", "VISA", "CREDIT", null);
    }

    private static CardGatewayPort.Result declined() {
        return new CardGatewayPort.Result(CardGatewayPort.Outcome.DECLINED, "pay_test", null,
                null, null, null, "51", "Insufficient funds", null, null, null, "CARD_DECLINED");
    }
    private static Payment persisted(PaymentMethod method) {
        Payment payment = Payment.create("idem-" + UUID.randomUUID(), GUARDIAN, OFFICER,
                method, PaymentType.PARTIAL, new BigDecimal("500.00"), "EGP");
        try {
            Field id = Payment.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(payment, UUID.randomUUID());

            Field createdAt = Payment.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(payment, Instant.now());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not fake Hibernate id assignment", e);
        }
        return payment;
    }
}