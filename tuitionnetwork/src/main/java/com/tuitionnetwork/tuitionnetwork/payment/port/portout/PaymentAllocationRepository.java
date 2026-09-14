package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.util.List;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;
public interface PaymentAllocationRepository {
    //Find all payments that have been applied to this particular fee.
    List<PaymentAllocation> saveAll(List<PaymentAllocation> allocations);
    List<PaymentAllocation> findByPaymentId(UUID paymentId);
    List<PaymentAllocation> findByFeeId(UUID feeId);
}
