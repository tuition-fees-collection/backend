package com.tuitionnetwork.tuitionnetwork.payment.saga;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStep;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.event.PaymentFailedEvent;
import com.tuitionnetwork.tuitionnetwork.payment.domain.event.PaymentSucceededEvent;
import com.tuitionnetwork.tuitionnetwork.payment.domain.event.ReceiptIssuedEvent;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.BillingPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.CardGatewayPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.OutboxEventPort;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentAllocationRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.ReceiptPort;
import tools.jackson.databind.json.JsonMapper;
@Component
public class PaymentSettlement {
    private static final Logger log = LoggerFactory.getLogger(PaymentSettlement.class);
    private final PaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final PaymentSaga saga;
    private final BillingPort billing;
    private final ReceiptPort receipts;
    private final OutboxEventPort outbox;
    private final JsonMapper json;

    public PaymentSettlement(PaymentRepository payments,
                             PaymentAllocationRepository allocations,
                             PaymentSaga saga,
                             BillingPort billing,
                             ReceiptPort receipts,
                             OutboxEventPort outbox,
                             JsonMapper json) {
        this.payments = payments;
        this.allocations = allocations;
        this.saga = saga;
        this.billing = billing;
        this.receipts = receipts;
        this.outbox = outbox;
        this.json = json;
    }
    @Transactional
    public Payment markPending(Payment payment) {
        payment.markPending();
        return payments.save(payment);
    }
    @Transactional
    public Payment settleSuccess(Payment payment, CardGatewayPort.Result result) {
    List<PaymentAllocation> rows = allocations.findByPaymentId(payment.getId());
    Map<UUID, BillingPort.PayableFee> current = billing
            .loadPayable(rows.stream().map(row -> row.getFeeId()).toList())
            .stream()
            .collect(Collectors.toMap(fee -> fee.feeId(), Function.identity()));

    PaymentSagaStep settle = saga.begin(payment.getId(), PaymentSagaStepType.SETTLE_FEES);
    billing.settle(payment.getId(), rows.stream()
            .map(row -> {
                BillingPort.PayableFee fee = current.get(row.getFeeId());
                if (fee == null) {
                    throw new IllegalStateException("Fee " + row.getFeeId()
                            + " is no longer payable at settlement time");
                }
                return new BillingPort.FeeSettlement(
                        row.getFeeId(), row.getAmountApplied(), fee.version());
            })
            .toList());
    saga.succeed(settle);

        payment.markSuccessful(
                result.gatewayPaymentId(),
                result.capturedAmount(),
                result.authCode(),
                result.rrn(),
                result.responseCode(),
                result.responseMessage(),
                result.maskedCardNumber(),
                result.cardScheme());
        Payment saved = payments.save(payment);
        PaymentSagaStep issue = saga.begin(saved.getId(), PaymentSagaStepType.ISSUE_RECEIPT);
        ReceiptPort.IssuedReceipt receipt = receipts.issueFor(saved.getId());
        saga.succeed(issue);

        outbox.save(OutboxEvent.of(saved.getId(), PaymentSucceededEvent.EVENT_TYPE,
                serialise(new PaymentSucceededEvent(
                        saved.getId(), saved.getGuardianId(), saved.getAmount(),
                        saved.getCurrency(), saved.getMethod(), saved.getMaskedCardNumber(),
                        saved.getSettledAt(),
                        rows.stream()
                                .map(row -> new PaymentSucceededEvent.SettledFee(
                                        row.getFeeId(), row.getAmountApplied()))
                                .toList()))));

        outbox.save(OutboxEvent.of(saved.getId(), ReceiptIssuedEvent.EVENT_TYPE,
                serialise(new ReceiptIssuedEvent(saved.getId(), receipt.receiptId(),
                        receipt.referenceNumber(), Instant.now()))));

        log.info("Payment {} settled {} {} across {} fee line(s), receipt {}",
                saved.getId(), saved.getAmount(), saved.getCurrency(),
                rows.size(), receipt.referenceNumber());

        return saved;
    }
    @Transactional
    public Payment settleFailure(Payment payment, String reason, CardGatewayPort.Result result) {
        payment.markFailed(reason, result == null ? null : result.responseCode());
        Payment saved = payments.save(payment);

        outbox.save(OutboxEvent.of(saved.getId(), PaymentFailedEvent.EVENT_TYPE,
                serialise(new PaymentFailedEvent(
                        saved.getId(), saved.getGuardianId(), saved.getAmount(),
                        saved.getCurrency(), saved.getMethod(), reason,
                        result == null ? null : result.responseCode(), Instant.now()))));

        log.info("Payment {} failed: {}", saved.getId(), reason);
        return saved;
    }
    @Transactional
    public Payment flagForReconciliation(Payment payment, CardGatewayPort.Result result) {
        payment.flagForReconciliation(result.responseMessage());
        Payment saved = payments.save(payment);

        log.error("Payment {} needs reconciliation: provider outcome {} - {}",
                saved.getId(), result.outcome(), result.responseMessage());

        return saved;
    }
    @Transactional
    public Payment attachGatewayId(Payment payment, String gatewayPaymentId) {
        payment.attachGatewayPaymentId(gatewayPaymentId);
        return payments.save(payment);
    }

    private String serialise(Object event) {
        try {
            return json.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialise outbox payload", e);
        }
    }
}