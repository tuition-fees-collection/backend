package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStep;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepStatus;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepType;
public interface PaymentSagaStepJpaRepository  extends JpaRepository<PaymentSagaStep,UUID>{
    List<PaymentSagaStep> findByPaymentId(UUID paymentId);
    Optional<PaymentSagaStep>findByPaymentIdAndStepName(UUID paymentId, PaymentSagaStepType stepName);
    List<PaymentSagaStep> findByStepNameInAndStatusAndAttemptedAtBefore(
            Collection<PaymentSagaStepType> stepNames,
            PaymentSagaStepStatus status,
            Instant cutoff);
    
}
