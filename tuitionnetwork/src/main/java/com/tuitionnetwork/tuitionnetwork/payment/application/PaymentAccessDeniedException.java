package com.tuitionnetwork.tuitionnetwork.payment.application;

 
import java.util.UUID;
 
/**
 * The caller asked about a payment, or tried to pay for a student, that is not
 * theirs. SYS-US-03 AC4.
 *
 * The message says "Payment not found" on purpose, and this should map to HTTP
 * 404 rather than 403. An error that distinguishes "exists but not yours" from
 * "no such payment" lets someone probe for valid ids by watching which response
 * they get. The guardianId is kept as a field so the audit log can record who
 * asked, without that detail reaching the response body.
 *
 * Thrown by PaymentService and PaymentQueryService, never by a controller - the
 * check belongs where the decision is made, so a second caller cannot skip it.
 */
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
 
    /** Who asked. For the audit trail, not for the response. */
    public UUID getGuardianId() {
        return guardianId;
    }
 
    /** The payment or student they asked about. May be null. */
    public UUID getResourceId() {
        return resourceId;
    }
}
