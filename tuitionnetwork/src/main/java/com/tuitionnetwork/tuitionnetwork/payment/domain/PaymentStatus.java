package com.tuitionnetwork.tuitionnetwork.payment.domain;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
public enum PaymentStatus {
    CREATED, // Payment has been created but not yet processed
    PENDING, // Payment is in the process of being completed
    SUCCESSFUL, // Payment has been successfully completed
    FAILED, // Payment has failed
    RECON_REQUIRED; // Payment requires reconciliation due to discrepancies
   private static final Set<PaymentStatus> NONE = Collections.emptySet();
 //allow transitions between states
    private Set<PaymentStatus> allowedTargets() {
        switch (this) {
            case CREATED:
                return EnumSet.of(PENDING, FAILED);
            case PENDING:
                return EnumSet.of(SUCCESSFUL, FAILED, RECON_REQUIRED);
            case RECON_REQUIRED:
                return EnumSet.of(SUCCESSFUL, FAILED);
            case SUCCESSFUL:
            case FAILED:
            default:
                return NONE;
        }
    }
 //verify if the transition to the target state is allowed
    public boolean canTransitionTo(PaymentStatus target) {
        return target != null && allowedTargets().contains(target);
    }
 
    //answer is payemnt finished
    public boolean isTerminal() {
        return this == SUCCESSFUL || this == FAILED;
    }
 
  //did we get money
    public boolean isSettled() {
        return this == SUCCESSFUL;
    }
 
// is still processing 
    public boolean isInFlight() {
        return this == CREATED || this == PENDING;
    }

}
