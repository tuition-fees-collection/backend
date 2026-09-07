package com.tuitionnetwork.tuitionnetwork.payment.infa.persistence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
public interface  paymentAllocationrepository  extends JpaRepository<PaymentAllocation, UUID> 
{
    List<PaymentAllocation> findByPaymentId(UUID paymentId); //fetch all payment allocations for a given payment
    List<PaymentAllocation> findByFeeId(UUID feeId); //fetch all payment allocations for a given fee
}
