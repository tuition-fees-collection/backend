package com.tuitionnetwork.tuitionnetwork.payment.reconciliation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import com.tuitionnetwork.tuitionnetwork.payment.saga.PaymentSettlement;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GatewayStatusCheckServiceTest {

    private static final UUID GUARDIAN = UUID.randomUUID();
    private static final UUID OFFICER = UUID.randomUUID();
    private static final UUID RUN_ID = UUID.randomUUID();

    @Mock private PaymentRepository payments;
    @Mock private CardGatewayPort gateway;
    @Mock private PaymentSettlement settlement;
    @Mock private GatewayCheckStore store;

    private GatewayStatusCheckService service;

    @BeforeEach
    void setUp() {
        service = new GatewayStatusCheckService(payments, gateway, settlement, store, 5, 24);
        when(store.startRun()).thenReturn(RUN_ID);
        when(store.itemsForPayment(any())).thenReturn(List.of());
    }
    @Nested
    @DisplayName("the provider says it was captured")
    class ProviderCaptured {
        @Test
        @DisplayName("settles the payment through the full path, not just a status flip")
        void resolvesToSuccess() {
            Payment flagged = flagged("pay_123");
            when(payments.findUnresolvedOlderThan(any())).thenReturn(List.of(flagged));
            when(gateway.getStatus("pay_123")).thenReturn(approved());
            service.sweep();
            verify(settlement).settleSuccess(eq(flagged), any());
            verify(store).recordItem(eq(RUN_ID), eq(flagged.getId()), eq("CAPTURED"),
                    eq(GatewayCheckRecords.Outcome.RESOLVED));
        }
    }

    @Nested
    @DisplayName("the provider says it was declined")
    class ProviderDeclined {

        @Test
        @DisplayName("fails the payment, and the fee was never touched so nothing is undone")
        void resolvesToFailure() {
            Payment flagged = flagged("pay_123");
            when(payments.findUnresolvedOlderThan(any())).thenReturn(List.of(flagged));
            when(gateway.getStatus("pay_123")).thenReturn(declined());

            service.sweep();

            verify(settlement).settleFailure(eq(flagged), anyString(), any());
            verify(settlement, never()).settleSuccess(any(), any());
            verify(store).recordItem(eq(RUN_ID), eq(flagged.getId()), any(),
                    eq(GatewayCheckRecords.Outcome.RESOLVED));
        }
    }

    @Nested
    @DisplayName("the provider has no answer")
    class NoAnswer {
        @Test
        @DisplayName("NOT_FOUND changes nothing and is left for a human")
        void notFoundDoesNotResolve() {
            Payment flagged = flagged("pay_123");
            when(payments.findUnresolvedOlderThan(any())).thenReturn(List.of(flagged));
            when(gateway.getStatus("pay_123")).thenReturn(
                    CardGatewayPort.Result.notFound("pay_123"));

            service.sweep();

            verify(settlement, never()).settleSuccess(any(), any());
            verify(settlement, never()).settleFailure(any(), anyString(), any());
            verify(store).recordItem(eq(RUN_ID), eq(flagged.getId()), any(),
                    eq(GatewayCheckRecords.Outcome.MISMATCHED));
        }

        @Test
        @DisplayName("an unreachable provider changes nothing and retries next sweep")
        void unknownDoesNotResolve() {
            Payment flagged = flagged("pay_123");
            when(payments.findUnresolvedOlderThan(any())).thenReturn(List.of(flagged));
            when(gateway.getStatus("pay_123")).thenReturn(
                    CardGatewayPort.Result.unknown("pay_123", "still unreachable"));

            service.sweep();

            verify(settlement, never()).settleSuccess(any(), any());
            verify(settlement, never()).settleFailure(any(), anyString(), any());
            verify(store).recordItem(eq(RUN_ID), eq(flagged.getId()), any(),
                    eq(GatewayCheckRecords.Outcome.UNMATCHED));
        }

        @Test
        @DisplayName("a payment still awaiting an OTP is not a mismatch")
        void pending3dsIsMatched() {
            Payment flagged = flagged("pay_123");
            when(payments.findUnresolvedOlderThan(any())).thenReturn(List.of(flagged));
            when(gateway.getStatus("pay_123")).thenReturn(
                    result(CardGatewayPort.Outcome.REQUIRES_3DS, "PENDING_3DS"));

            service.sweep();

            verify(store).recordItem(eq(RUN_ID), eq(flagged.getId()), eq("PENDING_3DS"),
                    eq(GatewayCheckRecords.Outcome.MATCHED));
            verify(settlement, never()).settleSuccess(any(), any());
        }
    }

    @Nested
    @DisplayName("what does not get swept")
    class Selection {
        @Test
        @DisplayName("a payment with no provider reference is recorded once, then left alone")
        void unreferencedRecordedOnce() {
            Payment noReference = flagged(null);
            when(payments.findUnresolvedOlderThan(any())).thenReturn(List.of(noReference));

            service.sweep();

            verify(gateway, never()).getStatus(anyString());
            verify(store).recordItem(eq(RUN_ID), eq(noReference.getId()), eq(null),
                    eq(GatewayCheckRecords.Outcome.MISMATCHED));
        }

        @Test
        @DisplayName("and is skipped entirely once it has already been recorded")
        void unreferencedSkippedSecondTime() {
            Payment noReference = flagged(null);
            when(payments.findUnresolvedOlderThan(any())).thenReturn(List.of(noReference));
            when(store.itemsForPayment(noReference.getId())).thenReturn(List.of(
                    new GatewayCheckRecords.Item(UUID.randomUUID(), RUN_ID, noReference.getId(),
                            null, GatewayCheckRecords.Outcome.MISMATCHED)));

            assertNull(service.sweep(), "nothing left to check means no run is opened at all");

            verify(store, never()).startRun();
            verify(store, never()).recordItem(any(), any(), any(), any());
        }

        @Test
        @DisplayName("no run row is created when there is nothing to resolve")
        void emptySweepWritesNothing() {
            when(payments.findUnresolvedOlderThan(any())).thenReturn(List.of());

            assertNull(service.sweep());

            verify(store, never()).startRun();
        }
    }

    @Nested
    @DisplayName("the run record")
    class RunRecord {

        @Test
        @DisplayName("counts what was checked and closes the run")
        void closesWithCounts() {
            Payment resolved = flagged("pay_1");
            Payment stillWaiting = flagged("pay_2");
            when(payments.findUnresolvedOlderThan(any()))
                    .thenReturn(List.of(resolved, stillWaiting));
            when(gateway.getStatus("pay_1")).thenReturn(approved());
            when(gateway.getStatus("pay_2")).thenReturn(
                    result(CardGatewayPort.Outcome.REQUIRES_3DS, "PENDING_3DS"));

            GatewayCheckRecords.Run run = service.sweep();

            assertEquals(1, run.matchedCount());
            assertEquals(1, run.unmatchedCount());
            assertEquals(2, run.checkedCount());
            verify(store).closeRun(RUN_ID, 1, 1);
        }
    }

    private static CardGatewayPort.Result approved() {
        return result(CardGatewayPort.Outcome.APPROVED, "CAPTURED");
    }

    private static CardGatewayPort.Result declined() {
        return new CardGatewayPort.Result(CardGatewayPort.Outcome.DECLINED, "pay_123", null,
                null, null, null, "51", "Insufficient funds", null, null, null, "CARD_DECLINED");
    }

    private static CardGatewayPort.Result result(CardGatewayPort.Outcome outcome, String status) {
        return new CardGatewayPort.Result(outcome, "pay_123", status,
                new BigDecimal("500.00"), "195000", "3355", "00", "Approved",
                "411111******1111", "VISA", "CREDIT", null);
    }
    private static Payment flagged(String gatewayPaymentId) {
        Payment payment = Payment.create("idem-" + UUID.randomUUID(), GUARDIAN, OFFICER,
                PaymentMethod.CREDIT_CARD, PaymentType.PARTIAL, new BigDecimal("500.00"), "EGP");
        try {
            Field id = Payment.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(payment, UUID.randomUUID());

            Field createdAt = Payment.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(payment, Instant.now().minusSeconds(600));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }

        payment.markPending();
        payment.flagForReconciliation("Provider did not respond");
        if (gatewayPaymentId != null) {
            payment.attachGatewayPaymentId(gatewayPaymentId);
        }
        return payment;
    }
}
