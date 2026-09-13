package com.tuitionnetwork.tuitionnetwork.payment.domain.event;
import java.time.Instant;
import java.util.UUID;
public record ReceiptIssuedEvent (
     UUID paymentId,
        UUID receiptId,
        String referenceNumber,
        Instant generatedAt
){
    public static final String EVENT_TYPE = "RECEIPT_ISSUED";
    public String eventType() {
        return EVENT_TYPE;
    }

    
}
