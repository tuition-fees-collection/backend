package com.tuitionnetwork.tuitionnetwork.payment.infra.presistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStep;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.OutboxEventJpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.PaymentAllocationJpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.PaymentJpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.PaymentSagaStepJpaRepository;
import jakarta.persistence.EntityManager;


@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentPersistenceIT {

    private static final UUID GUARDIAN = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OFFICER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID FEE = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Autowired private PaymentJpaRepository payments;
    @Autowired private PaymentAllocationJpaRepository allocations;
    @Autowired private PaymentSagaStepJpaRepository steps;
    @Autowired private OutboxEventJpaRepository outbox;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Test
    @DisplayName("payment status is stored as its name, not its ordinal")
    void statusStoredAsString() {
        Payment payment = payments.saveAndFlush(newPayment("idem-" + UUID.randomUUID()));
        entityManager.flush();

        String stored = jdbc.queryForObject(
                "SELECT status FROM payments.payment WHERE id = ?", String.class, payment.getId());

        assertEquals("CREATED", stored);
    }

    @Test
    @DisplayName("RECON_REQUIRED survives a round trip and fits the column")
    void reconRequiredFits() {
        Payment payment = newPayment("idem-" + UUID.randomUUID());
        payment.markPending();
        payment.flagForReconciliation("Provider did not respond");
        payments.saveAndFlush(payment);
        entityManager.flush();

        String stored = jdbc.queryForObject(
                "SELECT status FROM payments.payment WHERE id = ?", String.class, payment.getId());

        assertEquals("RECON_REQUIRED", stored);
        assertTrue(stored.length() <= 20, "the column is VARCHAR(20)");
    }

    @Test
    @DisplayName("saga step name and status are stored as names too")
    void sagaStepEnumsStoredAsStrings() {
        Payment payment = payments.saveAndFlush(newPayment("idem-" + UUID.randomUUID()));

        PaymentSagaStep step = PaymentSagaStep.plan(payment.getId(), PaymentSagaStepType.CHARGE);
        steps.saveAndFlush(step);
        entityManager.flush();

        assertEquals("CHARGE", jdbc.queryForObject(
                "SELECT step_name FROM payments.payment_saga_step WHERE id = ?",
                String.class, step.getId()));
        assertEquals("PENDING", jdbc.queryForObject(
                "SELECT status FROM payments.payment_saga_step WHERE id = ?",
                String.class, step.getId()));
    }
    @Test
    @DisplayName("a duplicate idempotency key is rejected by the database")
    void duplicateIdempotencyKeyRejected() {
        String key = "idem-" + UUID.randomUUID();
        payments.saveAndFlush(newPayment(key));

        assertThrows(DataIntegrityViolationException.class, () -> {
            payments.saveAndFlush(newPayment(key));
            entityManager.flush();
        });
    }
        @Test
    @DisplayName("outbox created_at and status are populated before insert")
    void outboxDefaultsArePopulated() {
        Payment payment = payments.saveAndFlush(newPayment("idem-" + UUID.randomUUID()));

        OutboxEvent event = OutboxEvent.of(payment.getId(), "PAYMENT_CREATED", "{\"a\":1}");
        outbox.saveAndFlush(event);
        entityManager.flush();

        assertNotNull(event.getCreatedAt());
        assertEquals("PENDING", jdbc.queryForObject(
                "SELECT status FROM payments.outbox_event WHERE id = ?",
                String.class, event.getId()));
    }
    @Test
    @DisplayName("an amount round-trips without losing precision")
    void amountKeepsItsScale() {
        Payment payment = newPayment("idem-" + UUID.randomUUID());
        payments.saveAndFlush(payment);
        entityManager.clear();

        Payment reloaded = payments.findById(payment.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("18000.55").compareTo(reloaded.getAmount()));
        assertEquals(2, reloaded.getAmount().scale());
    }

    @Test
    @DisplayName("an allocation round-trips and is found by payment")
    void allocationRoundTrip() {
        Payment payment = payments.saveAndFlush(newPayment("idem-" + UUID.randomUUID()));
        allocations.saveAndFlush(
                PaymentAllocation.of(payment.getId(), FEE, new BigDecimal("8000.00")));
        entityManager.clear();

        List<PaymentAllocation> found = allocations.findByPaymentId(payment.getId());
        assertEquals(1, found.size());
        assertEquals(0, new BigDecimal("8000.00").compareTo(found.get(0).getAmountApplied()));
    }
    @Test
    @DisplayName("the unresolved query includes RECON_REQUIRED, not just in-flight")
    void unresolvedQueryIncludesFlagged() {
        Payment flagged = newPayment("idem-" + UUID.randomUUID());
        flagged.markPending();
        flagged.flagForReconciliation("unknown");
        payments.saveAndFlush(flagged);
        entityManager.flush();

        List<Payment> found = payments.findByStatusInAndCreatedAtBefore(
                List.of(PaymentStatus.CREATED, PaymentStatus.PENDING, PaymentStatus.RECON_REQUIRED),
                java.time.Instant.now().plusSeconds(60));

        assertTrue(found.stream().anyMatch(p -> p.getId().equals(flagged.getId())),
                "a flagged payment must be visible to the status check");
    }

    @Test
    @DisplayName("the idempotency lookup is scoped by guardian")
    void idempotencyLookupIsScoped() {
        String key = "idem-" + UUID.randomUUID();
        payments.saveAndFlush(newPayment(key));
        entityManager.flush();

        assertTrue(payments.findByGuardianIdAndIdempotencyKey(GUARDIAN, key).isPresent());
        assertTrue(payments.findByGuardianIdAndIdempotencyKey(UUID.randomUUID(), key).isEmpty(),
                "another guardian must not be handed this payment");
    }

    @Test
    @DisplayName("unresolved charge steps are found by type, status and age")
    void unresolvedChargesFound() {
        Payment payment = payments.saveAndFlush(newPayment("idem-" + UUID.randomUUID()));
        PaymentSagaStep charge = PaymentSagaStep.plan(payment.getId(), PaymentSagaStepType.CHARGE);
        charge.start(java.time.Instant.now().minusSeconds(600));
        steps.saveAndFlush(charge);
        entityManager.flush();

        List<PaymentSagaStep> found = steps.findByStepNameInAndStatusAndAttemptedAtBefore(
                List.of(PaymentSagaStepType.CHARGE, PaymentSagaStepType.CONFIRM_3DS),
                PaymentSagaStepStatus.IN_PROGRESS,
                java.time.Instant.now());

        assertTrue(found.stream().anyMatch(s -> s.getId().equals(charge.getId())));
    }
    private static Payment newPayment(String idempotencyKey) {
        return Payment.create(idempotencyKey, GUARDIAN, OFFICER,
                PaymentMethod.CREDIT_CARD, PaymentType.PARTIAL,
                new BigDecimal("18000.55"), "EGP");
    }
}
