package com.tuitionnetwork.tuitionnetwork.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentType;

public record PaymentCreatedEvent(UUID paymentId,
        UUID guardianId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentType paymentType,
        Instant createdAt,
        Instant expiresAt) {

     public static final String EVENT_TYPE = "PAYMENT_CREATED";
 
    public String eventType() {
        return EVENT_TYPE;
    }
}
