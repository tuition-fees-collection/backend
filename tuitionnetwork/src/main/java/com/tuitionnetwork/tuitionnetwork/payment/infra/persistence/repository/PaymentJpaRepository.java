package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
public interface  PaymentJpaRepository extends JpaRepository<Payment, UUID> {
Optional<Payment> findByGuardianIdAndIdempotencyKey(UUID guardianId, String idempotencyKey);
Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
 List<Payment> findByGuardianIdOrderByCreatedAtDesc(UUID guardianId);
 List<Payment> findByProcessedByOrderByCreatedAtDesc(UUID processedBy);
 List<Payment> findByStatusInAndCreatedAtBefore(Collection<PaymentStatus> statuses, Instant cutoff);
         
}
