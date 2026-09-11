package com.tuitionnetwork.tuitionnetwork.payment.saga;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStep;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepType;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentSagaStepRepository;
@Component
public class PaymentSaga {
    private static final Logger log = LoggerFactory.getLogger(PaymentSaga.class);
    private final PaymentSagaStepRepository steps;
    public PaymentSaga(PaymentSagaStepRepository steps) {
        this.steps = steps;
    }
    public PaymentSagaStep begin(UUID paymentId, PaymentSagaStepType type) {
        PaymentSagaStep step = PaymentSagaStep.plan(paymentId, type);
        step.start(Instant.now());
        return steps.save(step);
    }
    public void succeed(PaymentSagaStep step) {
        step.complete(Instant.now());
        steps.save(step);
    }
    public void fail(PaymentSagaStep step, String reason) {
        log.warn("Saga step {} failed on payment {}: {}",
                step.getStepName(), step.getPaymentId(), reason);
        step.fail(reason, Instant.now());
        steps.save(step);
    }
    public void skip(UUID paymentId, PaymentSagaStepType type, String reason) {
        PaymentSagaStep step = PaymentSagaStep.plan(paymentId, type);
        step.skip(reason, Instant.now());
        steps.save(step);
    }
    public void leaveUnresolved(PaymentSagaStep step, String reason) {
        log.error("Saga step {} on payment {} is UNRESOLVED: {}. "
                        + "Money may have moved - leaving IN_PROGRESS for the status check.",
                step.getStepName(), step.getPaymentId(), reason);
    }
}
