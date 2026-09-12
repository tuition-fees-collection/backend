package com.tuitionnetwork.tuitionnetwork.payment.services;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.event.PaymentCreatedEvent;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase.CreatePaymentCommand;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase.FeeLine;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.CreatePaymentUseCase.PaymentOrderView;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.identity.service.PaymentCaller;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.BillingPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.OutboxEventPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentAllocationRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import tools.jackson.databind.json.JsonMapper;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {
    private static final UUID GUARDIAN = UUID.randomUUID();
    private static final UUID OFFICER = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID OTHER_STUDENT = UUID.randomUUID();
    private static final UUID TUITION = UUID.randomUUID();
    private static final UUID BUS = UUID.randomUUID();
    private static final UUID FOREIGN_FEE = UUID.randomUUID();
    private static final UUID SETTLED_FEE = UUID.randomUUID();
    @Mock private PaymentRepository payments;
    @Mock private PaymentAllocationRepository allocations;
    @Mock private BillingPort billing;
    @Mock private IdentityAccessService identity;
    @Mock private OutboxEventPort outbox;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, allocations, billing, identity, outbox,
                JsonMapper.builder().build());
        when(payments.findByGuardianIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(identity.authorizePaymentCreation(STUDENT))
                .thenReturn(new PaymentCaller(GUARDIAN, OFFICER, RoleCode.PAYER));
        when(identity.authorizePaymentCreation(OTHER_STUDENT))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied"));
        when(payments.save(any(Payment.class))).thenAnswer(call -> savedPayment(call.getArgument(0)));
    }
    @Nested
    @DisplayName("creating an order")
    class Creating {

        @Test
        @DisplayName("writes the payment, its allocations and one outbox event together")
        void writesAllThree() {
            billingReturns(payableFee(TUITION, STUDENT, "18000.00", "8000.00", "PARTIALLY_PAID"),
                    payableFee(BUS, STUDENT, "3500.00", "0.00", "OUTSTANDING"));

            PaymentOrderView view = service.create(command(PaymentType.PARTIAL,
                    line(TUITION, "8000.00"),
                    line(BUS, "2000.00")));

            assertEquals(PaymentStatus.CREATED, view.status());
            assertEquals(0, new BigDecimal("10000.00").compareTo(view.amount()));
            assertFalse(view.idempotentReplay());
            assertNotNull(view.expiresAt());
            verify(payments).save(any(Payment.class));
            ArgumentCaptor<List<PaymentAllocation>> rows = ArgumentCaptor.captor();
            verify(allocations).saveAll(rows.capture());
            assertEquals(2, rows.getValue().size());
            ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.captor();
            verify(outbox).save(event.capture());
            assertEquals(PaymentCreatedEvent.EVENT_TYPE, event.getValue().getEventType());
        }

        @Test
        @DisplayName("the amount comes from the fee lines, not from the caller")
        void amountIsDerived() {
            billingReturns(payableFee(TUITION, STUDENT, "18000.00", "8000.00", "PARTIALLY_PAID"));

            PaymentOrderView view = service.create(
                    command(PaymentType.PARTIAL, line(TUITION, "2500.00")));

            assertEquals(0, new BigDecimal("2500.00").compareTo(view.amount()));
        }

        @Test
        @DisplayName("the outbox payload carries no card data")
        void payloadIsSafe() {
            billingReturns(payableFee(BUS, STUDENT, "3500.00", "0.00", "OUTSTANDING"));
            service.create(command(PaymentType.FULL, line(BUS, "3500.00")));

            ArgumentCaptor<OutboxEvent> event = ArgumentCaptor.captor();
            verify(outbox).save(event.capture());

            String payload = event.getValue().getPayload();
            assertFalse(payload.contains("cvv"));
            assertFalse(payload.contains("cardNumber"));
            assertTrue(payload.contains("PARTIAL") || payload.contains("FULL"));
        }
    }
    @Nested
    @DisplayName("idempotency")
    class Idempotency {

        @Test
        @DisplayName("a replayed key returns the original payment and charges nothing new")
        void replayReturnsOriginal() {
            Payment original = savedPayment(Payment.create("idem-1", GUARDIAN, OFFICER,
                    PaymentMethod.CREDIT_CARD, PaymentType.FULL, new BigDecimal("3500.00"), "EGP"));

            when(payments.findByGuardianIdAndIdempotencyKey(GUARDIAN, "idem-1"))
                    .thenReturn(Optional.of(original));

            PaymentOrderView view = service.create(
                    commandWithKey("idem-1", PaymentType.FULL, line(BUS, "3500.00")));

            assertTrue(view.idempotentReplay(), "the caller must be able to tell this was a replay");
            assertEquals(original.getId(), view.paymentId());
            verify(payments, never()).save(any(Payment.class));
            verify(allocations, never()).saveAll(anyList());
            verify(outbox, never()).save(any(OutboxEvent.class));
        }
    }
    @Nested
    @DisplayName("access control")
    class Access {

        @Test
        @DisplayName("a fee belonging to another family's child is refused")
        void otherFamilysFee() {
            billingReturns(payableFee(FOREIGN_FEE, OTHER_STUDENT, "12000.00", "0.00", "OUTSTANDING"));

            assertThrows(PaymentAccessDeniedException.class,
                    () -> service.create(new CreatePaymentCommand("idem-" + UUID.randomUUID(),
                            OTHER_STUDENT, List.of(line(FOREIGN_FEE, "12000.00")),
                            PaymentMethod.CREDIT_CARD, PaymentType.FULL, "EGP")));

            verify(payments, never()).save(any(Payment.class));
        }
        @Test
        @DisplayName("an unknown fee id gets the same error as someone else's fee")
        void unknownFeeIsIndistinguishable() {
            billingReturns(); 

            assertThrows(PaymentAccessDeniedException.class,
                    () -> service.create(command(PaymentType.FULL, line(UUID.randomUUID(), "100.00"))));
        }
    }

    @Nested
    @DisplayName("what may be paid")
    class Payable {

        @Test
        @DisplayName("an already settled fee is refused")
        void settledFee() {
            billingReturns(payableFee(SETTLED_FEE, STUDENT, "1500.00", "1500.00", "SETTLED"));

            PaymentValidationAssert.assertCode("FEE_NOT_PAYABLE",
                    () -> service.create(command(PaymentType.FULL, line(SETTLED_FEE, "1500.00"))));
        }

        @Test
        @DisplayName("paying more than is outstanding is refused")
        void overpayment() {
            billingReturns(payableFee(TUITION, STUDENT, "18000.00", "8000.00", "PARTIALLY_PAID"));

            PaymentValidationAssert.assertCode("AMOUNT_EXCEEDS_OUTSTANDING",
                    () -> service.create(command(PaymentType.PARTIAL, line(TUITION, "10000.01"))));
        }

        @Test
        @DisplayName("paying exactly the outstanding balance is allowed")
        void exactOutstandingIsFine() {
            billingReturns(payableFee(TUITION, STUDENT, "18000.00", "8000.00", "PARTIALLY_PAID"));

            PaymentOrderView view = service.create(
                    command(PaymentType.FULL, line(TUITION, "10000.00")));

            assertEquals(0, new BigDecimal("10000.00").compareTo(view.amount()));
        }

        @Test
        @DisplayName("a FULL payment that does not clear the whole balance is refused")
        void fullMustClearEverything() {
            billingReturns(payableFee(TUITION, STUDENT, "18000.00", "8000.00", "PARTIALLY_PAID"));

            PaymentValidationAssert.assertCode("PARTIAL_AMOUNT_ON_FULL_PAYMENT",
                    () -> service.create(command(PaymentType.FULL, line(TUITION, "5000.00"))));
        }
        @Test
        @DisplayName("the same fee twice in one payment is refused")
        void duplicateFeeLine() {
            billingReturns(payableFee(BUS, STUDENT, "3500.00", "0.00", "OUTSTANDING"));

            PaymentValidationAssert.assertCode("DUPLICATE_FEE_LINE",
                    () -> service.create(command(PaymentType.PARTIAL,
                            line(BUS, "1000.00"), line(BUS, "2000.00"))));

            verify(payments, never()).save(any(Payment.class));
        }
    }
    static final class PaymentValidationAssert {
        static void assertCode(String expectedCode, Runnable action) {
            Paymentvalidationexception thrown =
                    assertThrows(Paymentvalidationexception.class, action::run);
            assertEquals(expectedCode, thrown.getCode());
        }
    }

    private void billingReturns(BillingPort.PayableFee... fees) {
        when(billing.loadPayable(anyList())).thenReturn(List.of(fees));
    }

    private static BillingPort.PayableFee payableFee(UUID feeId, UUID studentId,
                                                     String amount, String paid, String status) {
        return new BillingPort.PayableFee(feeId, studentId, UUID.randomUUID(),
                "TUITION", "2026-T1", new BigDecimal(amount), new BigDecimal(paid), status, 1);
    }

    private static FeeLine line(UUID feeId, String amount) {
        return new FeeLine(feeId, new BigDecimal(amount));
    }

    private static CreatePaymentCommand command(PaymentType type, FeeLine... lines) {
        return commandWithKey("idem-" + UUID.randomUUID(), type, lines);
    }
    private static CreatePaymentCommand commandWithKey(String key, PaymentType type, FeeLine... lines) {
        return new CreatePaymentCommand(key, STUDENT, List.of(lines),
                PaymentMethod.CREDIT_CARD, type, "EGP");
    }

    private static Payment savedPayment(Payment payment) {
        try {
            if (payment.getId() == null) {
                Field id = Payment.class.getDeclaredField("id");
                id.setAccessible(true);
                id.set(payment, UUID.randomUUID());

                Field createdAt = Payment.class.getDeclaredField("createdAt");
                createdAt.setAccessible(true);
                createdAt.set(payment, Instant.now());
            }
            return payment;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not fake Hibernate id assignment", e);
        }
    }
}