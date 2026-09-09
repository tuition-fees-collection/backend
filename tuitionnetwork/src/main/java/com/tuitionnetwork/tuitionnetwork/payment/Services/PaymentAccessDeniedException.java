package com.tuitionnetwork.tuitionnetwork.payment.Services;
import java.util.UUID;
public class PaymentAccessDeniedException extends RuntimeException {
 
    private final UUID guardianId;
    private final UUID resourceId;
 
    public PaymentAccessDeniedException(UUID guardianId, UUID resourceId) {
        super("Payment not found");
        this.guardianId = guardianId;
        this.resourceId = resourceId;
    }
 
    public PaymentAccessDeniedException(UUID guardianId) {
        this(guardianId, null);
    }
 
    public UUID getGuardianId() {
        return guardianId;
    }
 
    public UUID getResourceId() {
        return resourceId;
    }
}
