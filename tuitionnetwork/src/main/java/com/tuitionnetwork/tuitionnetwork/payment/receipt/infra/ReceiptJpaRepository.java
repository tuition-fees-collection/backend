package com.tuitionnetwork.tuitionnetwork.payment.receipt.infra;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.receipt.domain.Receipt;
public interface ReceiptJpaRepository extends JpaRepository<Receipt, UUID> {
    Optional<Receipt> findByPaymentId(UUID paymentId);
    boolean existsByReferenceNumber(String referenceNumber);
}
