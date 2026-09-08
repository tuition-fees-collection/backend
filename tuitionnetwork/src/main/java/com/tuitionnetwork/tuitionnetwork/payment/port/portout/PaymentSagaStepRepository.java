package com.tuitionnetwork.tuitionnetwork.payment.port.portout;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStep;

public interface PaymentSagaStepRepository {
     PaymentSagaStep save(PaymentSagaStep step);
    List<PaymentSagaStep> findByPaymentId(UUID paymentId);
    List<PaymentSagaStep> findUnresolvedChargesOlderThan(Instant cutoff);
}
