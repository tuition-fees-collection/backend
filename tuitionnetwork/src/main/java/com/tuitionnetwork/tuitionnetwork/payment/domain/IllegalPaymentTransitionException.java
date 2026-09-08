package com.tuitionnetwork.tuitionnetwork.payment.domain;

import java.util.UUID;

public class IllegalPaymentTransitionException extends RuntimeException {

    private final UUID paymentId;
    private final PaymentStatus from;
    private final PaymentStatus to;

    public IllegalPaymentTransitionException(UUID paymentId, PaymentStatus from, PaymentStatus to) {
        super(String.format("Payment %s cannot move from %s to %s", paymentId, from, to));
        this.paymentId = paymentId;
        this.from = from;
        this.to = to;
    }

    public UUID getPaymentId() { 
        return paymentId; 
    }
    public PaymentStatus getFrom() { 
        return from;
     }
    public PaymentStatus getTo() { 
        return to; 
    }
}
