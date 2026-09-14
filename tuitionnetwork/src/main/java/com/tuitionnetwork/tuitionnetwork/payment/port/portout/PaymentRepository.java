package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID paymentId);
    Optional<Payment> findByGuardianIdAndIdempotencyKey(UUID guardianId, String idempotencyKey);
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
    List<Payment> findByGuardianIdOrderByCreatedAtDesc(UUID guardianId);
    List<Payment> findByProcessedByOrderByCreatedAtDesc(UUID processedBy);
    //Find payments that are still being processed and have been in that state longer than the specified time.
    List<Payment> findInFlightOlderThan(Instant cutoff);
    //Find payments whose result is still unresolved and have been unresolved for longer than the cutoff.
    List<Payment> findUnresolvedOlderThan(Instant cutoff);
}
