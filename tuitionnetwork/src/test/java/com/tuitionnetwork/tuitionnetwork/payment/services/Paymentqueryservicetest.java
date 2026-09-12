package com.tuitionnetwork.tuitionnetwork.payment.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.tuitionnetwork.tuitionnetwork.identity.domain.ActorType;
import com.tuitionnetwork.tuitionnetwork.identity.domain.AuthenticatedActor;
import com.tuitionnetwork.tuitionnetwork.identity.domain.RoleCode;
import com.tuitionnetwork.tuitionnetwork.identity.service.IdentityAccessService;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.GetPaymentUseCase.PaymentSummary;
import com.tuitionnetwork.tuitionnetwork.payment.port.in.GetPaymentUseCase.PaymentView;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.BillingPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentAllocationRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.ReceiptPort;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentQueryServiceTest {
    private static final UUID GUARDIAN = UUID.randomUUID();
    private static final UUID OTHER_GUARDIAN = UUID.randomUUID();
    private static final UUID OFFICER = UUID.randomUUID();
    private static final UUID FEE = UUID.randomUUID();
    @Mock private PaymentRepository payments;
    @Mock private PaymentAllocationRepository allocations;
    @Mock private BillingPort billing;
    @Mock private ReceiptPort receipts;
    @Mock private IdentityAccessService identity;

    private PaymentQueryService service;
    private Payment payment;

    @BeforeEach
    void setUp() {
        service = new PaymentQueryService(payments, allocations, billing, receipts, identity);
        when(identity.currentActor()).thenReturn(payer(GUARDIAN));

        payment = successful();
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(allocations.findByPaymentId(any())).thenReturn(List.of(
                PaymentAllocation.of(payment.getId(), FEE, new BigDecimal("500.00"))));
        when(billing.loadPayable(anyList())).thenReturn(List.of(
                new BillingPort.PayableFee(FEE, UUID.randomUUID(), UUID.randomUUID(),
                        "BUS", "2026-T1", new BigDecimal("3500.00"), new BigDecimal("3500.00"),
                        "SETTLED", 4)));
        when(receipts.findByPaymentId(any())).thenReturn(Optional.of(
                new ReceiptPort.IssuedReceipt(UUID.randomUUID(), payment.getId(), "RCP-2026-TEST")));
    }

    @Nested
    @DisplayName("access control")
    class Access {
        @Test
        @DisplayName("another guardian cannot read this payment")
        void wrongGuardianRefused() {
            org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("Access denied"))
                    .when(identity).authorizePaymentAccess(GUARDIAN);
            assertThrows(PaymentAccessDeniedException.class,
                    () -> service.getById(payment.getId()));
        }
        @Test
        @DisplayName("an unknown id is indistinguishable from someone else's")
        void unknownIdLooksTheSame() {
            UUID missing = UUID.randomUUID();
            when(payments.findById(missing)).thenReturn(Optional.empty());

            assertThrows(PaymentAccessDeniedException.class,
                    () -> service.getById(missing));
        }

        @Test
        @DisplayName("the owner can read it")
        void ownerAllowed() {
            PaymentView view = service.getById(payment.getId());
            assertEquals(payment.getId(), view.paymentId());
        }
    }

    @Nested
    @DisplayName("what a payment view carries")
    class Views {

        @Test
        @DisplayName("the allocations name the fee they cleared")
        void allocationsAreNamed() {
            PaymentView view = service.getById(payment.getId());

            assertEquals(1, view.allocations().size());
            assertEquals("BUS", view.allocations().get(0).feeType());
            assertEquals("2026-T1", view.allocations().get(0).collectionPeriod());
            assertEquals(0, new BigDecimal("500.00")
                    .compareTo(view.allocations().get(0).amountApplied()));
        }

        @Test
        @DisplayName("a settled payment carries its receipt reference")
        void receiptReference() {
            assertEquals("RCP-2026-TEST", service.getById(payment.getId()).receiptReference());
        }

        @Test
        @DisplayName("the masked card is shown, never anything else about the card")
        void maskedCardOnly() {
            PaymentView view = service.getById(payment.getId());
            assertTrue(view.maskedCardNumber().contains("*"));
        }
    }

    @Nested
    @DisplayName("history")
    class History {

        @Test
        @DisplayName("carries the receipt number for settled payments")
        void settledCarriesReceipt() {
            when(payments.findByGuardianIdOrderByCreatedAtDesc(GUARDIAN))
                    .thenReturn(List.of(payment));

            List<PaymentSummary> history = service.history();

            assertEquals(1, history.size());
            assertEquals("RCP-2026-TEST", history.get(0).receiptReference());
            assertTrue(history.get(0).hasReceipt());
        }
        @Test
        @DisplayName("does not look up a receipt for a payment that never succeeded")
        void unsettledSkipsTheLookup() {
            Payment failed = failed();
            when(payments.findByGuardianIdOrderByCreatedAtDesc(GUARDIAN))
                    .thenReturn(List.of(failed));

            List<PaymentSummary> history = service.history();

            assertNull(history.get(0).receiptReference());
            verify(receipts, never()).findByPaymentId(failed.getId());
        }
    }
    private static Payment successful() {
        Payment payment = persisted();
        payment.markPending();
        payment.markSuccessful("pay_1", new BigDecimal("500.00"), "195000", "3355",
                "00", "Approved", "411111******1111", "VISA");
        return payment;
    }

    private static Payment failed() {
        Payment payment = persisted();
        payment.markPending();
        payment.markFailed("declined", "51");
        return payment;
    }
    private static Payment persisted() {
        Payment payment = Payment.create("idem-" + UUID.randomUUID(), GUARDIAN, OFFICER,
                PaymentMethod.CREDIT_CARD, PaymentType.PARTIAL, new BigDecimal("500.00"), "EGP");
        try {
            Field id = Payment.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(payment, UUID.randomUUID());

            Field createdAt = Payment.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(payment, Instant.now());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return payment;
    }

    private static AuthenticatedActor payer(UUID guardianId) {
        return new AuthenticatedActor(UUID.randomUUID(), guardianId,
                ActorType.GUARDIAN, RoleCode.PAYER, null, null);
    }
}