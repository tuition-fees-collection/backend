package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.domain.Payment;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
import java.util.List;
import java.util.Optional;
public interface PaymentJpaRepository  extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey); //find payment by idempotency key to prevent duplicates
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId); //find payment by gateway payment id
    List<Payment> findByGuardianId( UUID guardianId); //fetch all payments for a given guardian
    List<Payment> findByStatus(PaymentStatus status); //fetch all payments with a given status
    

}
