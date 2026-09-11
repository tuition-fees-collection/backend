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
        public BigDecimal outstanding() {
            return amount.subtract(amountPaid);
        }
 
        public boolean isPayable() {
            return "OUTSTANDING".equals(status) || "PARTIALLY_PAID".equals(status);
        }
    }

    record FeeSettlement(UUID feeId, BigDecimal amountApplied, int expectedVersion) {
    }
}