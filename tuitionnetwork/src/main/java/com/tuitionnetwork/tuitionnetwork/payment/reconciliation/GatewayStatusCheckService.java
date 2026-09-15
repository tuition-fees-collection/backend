package com.tuitionnetwork.tuitionnetwork.payment.reconciliation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.Backofficegatewayport;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import com.tuitionnetwork.tuitionnetwork.payment.saga.PaymentSettlement;
@Service
public class GatewayStatusCheckService {

    private static final Logger log = LoggerFactory.getLogger(GatewayStatusCheckService.class);

    private final PaymentRepository payments;
    private final CardGatewayPort gateway;
    private final Backofficegatewayport backoffice;
    private final PaymentSettlement settlement;
    private final GatewayCheckStore store;
    private final Duration staleAfter;
    private final Duration giveUpAfter;

    public GatewayStatusCheckService(
            PaymentRepository payments,
            CardGatewayPort gateway,
            Backofficegatewayport backoffice,
            PaymentSettlement settlement,
            GatewayCheckStore store,
            @Value("${payments.reconciliation.stale-after-minutes:5}") int staleAfterMinutes,
            @Value("${payments.reconciliation.give-up-after-hours:24}") int giveUpAfterHours) {
        this.payments = payments;
        this.gateway = gateway;
        this.backoffice = backoffice;
        this.settlement = settlement;
        this.store = store;
        this.staleAfter = Duration.ofMinutes(staleAfterMinutes);
        this.giveUpAfter = Duration.ofHours(giveUpAfterHours);
    }
    @Scheduled(fixedDelayString = "${payments.reconciliation.interval-ms:120000}")
    public GatewayCheckRecords.Run sweep() {

        List<Payment> candidates = findUnresolved();
        List<Payment> uncheckable = findUncheckable();

        if (candidates.isEmpty() && uncheckable.isEmpty()) {
            log.debug("Gateway status check: nothing to resolve");
            return null;
        }

        UUID runId = store.startRun();
        int matched = 0;
        int unmatched = 0;

        for (Payment payment : candidates) {
            if (check(runId, payment)) {
                matched++;
            } else {
                unmatched++;
            }
        }
        for (Payment payment : uncheckable) {
            log.warn("Payment {} is {} with no provider reference - recording once, "
                            + "it cannot be resolved automatically",
                    payment.getId(), payment.getStatus());
            store.recordItem(runId, payment.getId(), null, GatewayCheckRecords.Outcome.MISMATCHED);
            unmatched++;
        }

        store.closeRun(runId, matched, unmatched);
        log.info("Gateway status check {}: {} checked, {} matched, {} unmatched",
                runId, candidates.size() + uncheckable.size(), matched, unmatched);

        return new GatewayCheckRecords.Run(runId, Instant.now(), matched, unmatched);
    }
    private List<Payment> findUnresolved() {
        Instant now = Instant.now();
        Instant tooNew = now.minus(staleAfter);
        Instant tooOld = now.minus(giveUpAfter);

        return payments.findUnresolvedOlderThan(tooNew).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.RECON_REQUIRED
                        || payment.getStatus() == PaymentStatus.PENDING)
                .filter(payment -> payment.getCreatedAt().isAfter(tooOld))
                .filter(payment -> payment.getGatewayPaymentId() != null)
                .toList();
    }
    private List<Payment> findUncheckable() {
        Instant tooNew = Instant.now().minus(staleAfter);

        return payments.findUnresolvedOlderThan(tooNew).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.RECON_REQUIRED
                        || payment.getStatus() == PaymentStatus.PENDING)
                .filter(payment -> payment.getGatewayPaymentId() == null)
                .filter(payment -> store.itemsForPayment(payment.getId()).isEmpty())
                .toList();
    }

    /** @return true when our record and the provider's already agreed. */
    private boolean check(UUID runId, Payment payment) {
        if (payment.getGatewayPaymentId() == null) {
            store.recordItem(runId, payment.getId(), null, GatewayCheckRecords.Outcome.MISMATCHED);
            return false;
        }
        CardGatewayPort.Result actual = payment.getMethod().usesHeldSource()
                ? backoffice.getSourcePaymentStatus(payment.getGatewayPaymentId())
                : gateway.getStatus(payment.getGatewayPaymentId());

        switch (actual.outcome()) {

            case APPROVED -> {
                log.warn("Payment {} was CAPTURED at the provider but not settled here - resolving",
                        payment.getId());
                settlement.settleSuccess(payment, actual);
                store.recordItem(runId, payment.getId(), actual.gatewayStatus(),
                        GatewayCheckRecords.Outcome.RESOLVED);
                return false;
            }

            case DECLINED, REJECTED -> {
                log.info("Payment {} was declined at the provider - resolving to FAILED",
                        payment.getId());
                settlement.settleFailure(payment, "Confirmed declined by the provider", actual);
                store.recordItem(runId, payment.getId(), actual.gatewayStatus(),
                        GatewayCheckRecords.Outcome.RESOLVED);
                return false;
            }

            case REQUIRES_3DS -> {
                store.recordItem(runId, payment.getId(), actual.gatewayStatus(),
                        GatewayCheckRecords.Outcome.MATCHED);
                return true;
            }

            case NOT_FOUND -> {
                log.error("Payment {} is unknown at the provider - NOT assuming it failed",
                        payment.getId());
                store.recordItem(runId, payment.getId(), null,
                        GatewayCheckRecords.Outcome.MISMATCHED);
                return false;
            }

            default -> {
                store.recordItem(runId, payment.getId(), null,
                        GatewayCheckRecords.Outcome.UNMATCHED);
                return false;
            }
        }
    }
}