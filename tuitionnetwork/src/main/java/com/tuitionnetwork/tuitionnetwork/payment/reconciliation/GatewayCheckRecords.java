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
        MATCHED,
        RESOLVED,
        MISMATCHED,
        UNMATCHED
    }
}
