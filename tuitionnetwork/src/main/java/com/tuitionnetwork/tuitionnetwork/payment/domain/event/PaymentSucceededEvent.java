package com.tuitionnetwork.tuitionnetwork.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;

public record PaymentSucceededEvent (UUID paymentId,
        UUID guardianId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        String maskedCardNumber,
        Instant settledAt,
        List<SettledFee> settledFees){
            public static final String EVENT_TYPE = "PAYMENT_SUCCEEDED";
            public record SettledFee(UUID feeId, BigDecimal amountApplied) {
    }
    public PaymentSucceededEvent {
        settledFees = settledFees == null ? List.of() : List.copyOf(settledFees);
    }
 
    public String eventType() {
        return EVENT_TYPE;
    }
}
