package com.tuitionnetwork.tuitionnetwork.payment.infa.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStep;

public interface paymentSagaSteprepository  extends JpaRepository<PaymentSagaStep, UUID>{
    List<PaymentSagaStep> findByPaymentId(UUID paymentId);
    Optional<PaymentSagaStep> findByPaymentIdAndStepName(UUID paymentId, String stepName);
    
}
