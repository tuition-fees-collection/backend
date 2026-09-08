package com.tuitionnetwork.tuitionnetwork.payment.port.portout;

import java.util.List;
import java.util.UUID;

import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentAllocation;

public interface PaymentAllocationRepository {
    List<PaymentAllocation> saveAll(List<PaymentAllocation> allocations);
 
    List<PaymentAllocation> findByPaymentId(UUID paymentId);
 
    /** For the receipt: which fees did this payment clear, and by how much. */
    List<PaymentAllocation> findByFeeId(UUID feeId);
}
