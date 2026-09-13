package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.adapter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.PaymentAllocationJpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.PaymentAllocationRepository;
@Component 
public class PaymentAllocationPersistenceAdapter implements PaymentAllocationRepository {
    private final PaymentAllocationJpaRepository jpa;
    public PaymentAllocationPersistenceAdapter(PaymentAllocationJpaRepository jpa){
        this.jpa = jpa;
    }
      @Override
    public List<PaymentAllocation> saveAll(List<PaymentAllocation> allocations) {
        return jpa.saveAll(allocations);
    }
 
    @Override
    public List<PaymentAllocation> findByPaymentId(UUID paymentId) {
        return jpa.findByPaymentId(paymentId);
    }
 
    @Override
    public List<PaymentAllocation> findByFeeId(UUID feeId) {
        return jpa.findByFeeId(feeId);
    }
}
