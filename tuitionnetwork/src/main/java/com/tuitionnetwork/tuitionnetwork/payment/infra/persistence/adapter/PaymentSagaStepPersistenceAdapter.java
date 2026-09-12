package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.adapter;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStep;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepType;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.PaymentSagaStepJpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentSagaStepRepository;

@Component
public class PaymentSagaStepPersistenceAdapter implements PaymentSagaStepRepository {
    private static final EnumSet<PaymentSagaStepType> MONEY_MOVING =
            EnumSet.of(PaymentSagaStepType.CHARGE, PaymentSagaStepType.CONFIRM_3DS);
 
    private final PaymentSagaStepJpaRepository jpa;
 
    public PaymentSagaStepPersistenceAdapter(PaymentSagaStepJpaRepository jpa) {
        this.jpa = jpa;
    }
 @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentSagaStep save(PaymentSagaStep step) {
        return jpa.saveAndFlush(step);
    }
 
    @Override
    public List<PaymentSagaStep> findByPaymentId(UUID paymentId) {
        return jpa.findByPaymentId(paymentId);
    }
 
    @Override
    public List<PaymentSagaStep> findUnresolvedChargesOlderThan(Instant cutoff) {
        return jpa.findByStepNameInAndStatusAndAttemptedAtBefore(
                MONEY_MOVING, PaymentSagaStepStatus.IN_PROGRESS, cutoff);
    }
}
