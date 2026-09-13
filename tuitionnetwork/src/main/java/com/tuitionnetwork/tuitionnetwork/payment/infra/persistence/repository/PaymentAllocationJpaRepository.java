package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
public interface  PaymentAllocationJpaRepository  extends JpaRepository<PaymentAllocation,UUID>{
    List<PaymentAllocation> findByPaymentId(UUID paymentId);
     List<PaymentAllocation> findByFeeId(UUID feeId);
    
}
