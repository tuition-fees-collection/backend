package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID paymentId);
    Optional<Payment> findByGuardianIdAndIdempotencyKey(UUID guardianId, String idempotencyKey); //prevent duplicates
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId); //rec
    List<Payment> findByGuardianIdOrderByCreatedAtDesc(UUID guardianId);
    List<Payment> findByProcessedByOrderByCreatedAtDesc(UUID processedBy);
    List<Payment> findInFlightOlderThan(Instant cutoff);
    List<Payment> findUnresolvedOlderThan(Instant cutoff); //ttoo long
}
