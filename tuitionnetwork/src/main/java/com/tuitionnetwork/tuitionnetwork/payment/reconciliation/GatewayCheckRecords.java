package com.tuitionnetwork.tuitionnetwork.payment.reconciliation;
import java.time.Instant;
import java.util.UUID;
public final class GatewayCheckRecords {
    private GatewayCheckRecords() {
    }
    public record Run(UUID id, Instant runDate, int matchedCount, int unmatchedCount) {

        public int checkedCount() {
            return matchedCount + unmatchedCount;
        }
    }
    public record Item(UUID id, UUID runId, UUID paymentId, String gatewayStatus, Outcome outcome) {
    }
    public enum Outcome {
        MATCHED, //Everything agrees.
        RESOLVED, //There was a problem/, but the reconciliation process fixed or resolved it.
        MISMATCHED, //Both systems have information about the payment, but the information doesn't agree.
        UNMATCHED //The reconciliation process couldn't find a corresponding payment/record on the other side.
    }
}
