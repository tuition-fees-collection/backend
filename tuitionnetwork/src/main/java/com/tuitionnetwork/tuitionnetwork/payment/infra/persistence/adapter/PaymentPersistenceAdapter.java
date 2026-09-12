package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.adapter;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.PaymentJpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentRepository;
@Component 
public class PaymentPersistenceAdapter  implements PaymentRepository{
    private static final EnumSet<PaymentStatus> IN_FLIGHT =
            EnumSet.of(PaymentStatus.CREATED, PaymentStatus.PENDING);
             private final PaymentJpaRepository jpa;
             public PaymentPersistenceAdapter(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }
    private static final EnumSet<PaymentStatus> UNRESOLVED =
            EnumSet.of(PaymentStatus.CREATED, PaymentStatus.PENDING, PaymentStatus.RECON_REQUIRED);

    @Override
    public List<Payment> findUnresolvedOlderThan(Instant cutoff) {
        return jpa.findByStatusInAndCreatedAtBefore(UNRESOLVED, cutoff);
    }
 
    @Override
    public Payment save(Payment payment) {
        return jpa.save(payment);
    }
 
    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return jpa.findById(paymentId);
    }
 
    @Override
    public Optional<Payment> findByGuardianIdAndIdempotencyKey(UUID guardianId, String idempotencyKey) {
        return jpa.findByGuardianIdAndIdempotencyKey(guardianId, idempotencyKey);
    }
 
    @Override
    public Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId) {
        return jpa.findByGatewayPaymentId(gatewayPaymentId);
    }
 
    @Override
    public List<Payment> findByGuardianIdOrderByCreatedAtDesc(UUID guardianId) {
        return jpa.findByGuardianIdOrderByCreatedAtDesc(guardianId);
    }

    @Override
    public List<Payment> findByProcessedByOrderByCreatedAtDesc(UUID processedBy) {
        return jpa.findByProcessedByOrderByCreatedAtDesc(processedBy);
    }
    @Override
    public List<Payment> findInFlightOlderThan(Instant cutoff) {
        return jpa.findByStatusInAndCreatedAtBefore(IN_FLIGHT, cutoff);
    }
    
}
