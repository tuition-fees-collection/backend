package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.util.Optional;
import java.util.UUID;
public  interface  ReceiptPort {
 IssuedReceipt issueFor(UUID paymentId);
    Optional<IssuedReceipt> findByPaymentId(UUID paymentId);
    record IssuedReceipt(UUID receiptId, UUID paymentId, String referenceNumber) {
    }
    
}
