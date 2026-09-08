package com.tuitionnetwork.tuitionnetwork.payment.port.portout;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BillingPort {
    List<PayableFee> loadPayable(List<UUID> feeIds);
    void settle(UUID paymentId, List<FeeSettlement> settlements);
    record PayableFee(
            UUID feeId,
            UUID studentId,
            UUID institutionId,
            String feeType,
            String collectionPeriod,
            BigDecimal amount,
            BigDecimal amountPaid,
            String status,
            int version
    ) {
        /** What is still owed on this line. */
        public BigDecimal outstanding() {
            return amount.subtract(amountPaid);
        }
 
        public boolean isPayable() {
            return "OUTSTANDING".equals(status) || "PARTIALLY_PAID".equals(status);
        }
    }
 
    /** How much of one fee line a payment cleared. Mirrors PaymentAllocation. */
    record FeeSettlement(UUID feeId, BigDecimal amountApplied, int expectedVersion) {
    }
}