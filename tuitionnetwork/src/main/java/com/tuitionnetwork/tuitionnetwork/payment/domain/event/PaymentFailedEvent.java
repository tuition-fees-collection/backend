package com.tuitionnetwork.tuitionnetwork.payment.domain.event;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
public record PaymentFailedEvent(
      UUID paymentId,
        UUID guardianId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        String reason,
        String responseCode,
        Instant failedAt
) {
    public static final String EVENT_TYPE = "PAYMENT_FAILED";
    public String eventType() {
        return EVENT_TYPE;
    }
}
    
