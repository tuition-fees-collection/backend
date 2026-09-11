package com.tuitionnetwork.tuitionnetwork.payment.infra.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.BillingPort;
@SpringBootTest
class ConcurrentSettlementIT {
    private static final UUID STUDENT = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID INSTITUTION = UUID.fromString("55555555-5555-5555-5555-555555555555");
    @Autowired private BillingPort billing;
    @Autowired private JdbcTemplate jdbc;
    private UUID feeId;
    @BeforeEach
    void createFee() {
        feeId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO billing.fee
                    (id, student_id, institution_id, fee_type, collection_period,
                     currency, amount, amount_paid, status, version)
                VALUES (?, ?, ?, 'TUITION', '2026-RACE', 'EGP', 10000.00, 0.00, 'OUTSTANDING', 0)
                """, feeId, STUDENT, INSTITUTION);
    }
    @AfterEach
    void removeFee() {
        jdbc.update("DELETE FROM billing.fee WHERE id = ?", feeId);
    }
    @Test
    @DisplayName("two payments settling the same fee at once: one wins, one is rejected")
    void onlyOneSettlementWins() throws Exception {
        List<BillingPort.PayableFee> before = billing.loadPayable(List.of(feeId));
        assertEquals(1, before.size());
        int version = before.get(0).version();
        CountDownLatch bothReady = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();

        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            threads.submit(settleTask(version, bothReady, go, firstFailure));
            threads.submit(settleTask(version, bothReady, go, secondFailure));

            assertTrue(bothReady.await(5, TimeUnit.SECONDS), "both threads should reach the line");
            go.countDown();

            threads.shutdown();
            assertTrue(threads.awaitTermination(10, TimeUnit.SECONDS), "both threads should finish");
        } finally {
            threads.shutdownNow();
        }
        long failures = (firstFailure.get() == null ? 0 : 1) + (secondFailure.get() == null ? 0 : 1);
        assertEquals(1, failures,
                "exactly one settlement must be rejected - 0 means the fee was paid twice, "
                        + "2 means neither payment settled");

        Throwable rejected = firstFailure.get() != null ? firstFailure.get() : secondFailure.get();
        assertNotNull(rejected);
        assertTrue(rejected.getMessage().contains("modified by another payment"),
                "the loser must be told the fee moved under it, not fail for some other reason");
    }
    @Test
    @DisplayName("the fee is never paid twice")
    void balanceMovesExactlyOnce() throws Exception {
        int version = billing.loadPayable(List.of(feeId)).get(0).version();
        CountDownLatch bothReady = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            threads.submit(settleTask(version, bothReady, go, new AtomicReference<>()));
            threads.submit(settleTask(version, bothReady, go, new AtomicReference<>()));
            bothReady.await(5, TimeUnit.SECONDS);
            go.countDown();
            threads.shutdown();
            threads.awaitTermination(10, TimeUnit.SECONDS);
        } finally {
            threads.shutdownNow();
        }

        BigDecimal paid = jdbc.queryForObject(
                "SELECT amount_paid FROM billing.fee WHERE id = ?", BigDecimal.class, feeId);
        Integer finalVersion = jdbc.queryForObject(
                "SELECT version FROM billing.fee WHERE id = ?", Integer.class, feeId);

        assertEquals(0, new BigDecimal("1000.00").compareTo(paid),
                "1000 was settled once, not twice");
        assertEquals(version + 1, finalVersion,
                "the version moved once, so exactly one write landed");
    }
    @Test
    @DisplayName("a stale version is rejected even without a race")
    void staleVersionRejected() {
        int version = billing.loadPayable(List.of(feeId)).get(0).version();

        billing.settle(UUID.randomUUID(), List.of(
                new BillingPort.FeeSettlement(feeId, new BigDecimal("1000.00"), version)));
        Throwable rejected = null;
        try {
            billing.settle(UUID.randomUUID(), List.of(
                    new BillingPort.FeeSettlement(feeId, new BigDecimal("1000.00"), version)));
        } catch (RuntimeException e) {
            rejected = e;
        }

        assertNotNull(rejected, "settling on a version that has moved must be refused");
        assertTrue(rejected.getMessage().contains("modified by another payment"));
    }

    private Runnable settleTask(int version, CountDownLatch ready, CountDownLatch go,
                                AtomicReference<Throwable> failure) {
        return () -> {
            try {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                billing.settle(UUID.randomUUID(), List.of(
                        new BillingPort.FeeSettlement(feeId, new BigDecimal("1000.00"), version)));
            } catch (Throwable t) {
                failure.set(t);
            }
        };
    }
}
