package com.tuitionnetwork.tuitionnetwork.payment.event;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.payment.domain.event.PaymentCreatedEvent;
import com.tuitionnetwork.tuitionnetwork.payment.domain.event.PaymentFailedEvent;
import com.tuitionnetwork.tuitionnetwork.payment.domain.event.PaymentSucceededEvent;
import com.tuitionnetwork.tuitionnetwork.payment.domain.event.ReceiptIssuedEvent;

class PaymentEventsTest {

    private static final UUID PAYMENT = UUID.randomUUID();
    private static final UUID GUARDIAN = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Test
    @DisplayName("every event type fits outbox_event.event_type VARCHAR(50)")
    void eventTypesFitColumn() {
        for (String type : List.of(
                PaymentCreatedEvent.EVENT_TYPE,
                PaymentSucceededEvent.EVENT_TYPE,
                PaymentFailedEvent.EVENT_TYPE,
                ReceiptIssuedEvent.EVENT_TYPE)) {
            assertTrue(type.length() <= 50, type + " is " + type.length() + " chars");
        }
    }

    @Test
    @DisplayName("settled fees are copied so an event cannot be mutated after construction")
    void settledFeesAreImmutable() {
        List<PaymentSucceededEvent.SettledFee> fees = new ArrayList<>();
        fees.add(new PaymentSucceededEvent.SettledFee(UUID.randomUUID(), new BigDecimal("8000.00")));

        PaymentSucceededEvent event = succeeded(fees);

        fees.add(new PaymentSucceededEvent.SettledFee(UUID.randomUUID(), new BigDecimal("1.00")));
        assertEquals(1, event.settledFees().size(), "the event must not see later changes to the source list");

        assertThrows(UnsupportedOperationException.class,
                () -> event.settledFees().add(null));
    }

    @Test
    @DisplayName("a null fee list becomes empty rather than an NPE in another module")
    void nullFeesBecomeEmpty() {
        assertEquals(List.of(), succeeded(null).settledFees());
    }

    @Test
    @DisplayName("a cash success carries no card number at all")
    void cashCarriesNoCardData() {
        PaymentSucceededEvent event = new PaymentSucceededEvent(PAYMENT, GUARDIAN,
                new BigDecimal("300.00"), "EGP", PaymentMethod.CASH, null, NOW, List.of());
        assertEquals(null, event.maskedCardNumber());
    }

    @Test
    @DisplayName("a card success carries the masked number, never a full PAN")
    void cardCarriesOnlyMaskedNumber() {
        PaymentSucceededEvent event = succeeded(List.of());
        assertTrue(event.maskedCardNumber().contains("*"),
                "the event must carry the gateway's masked value, not a raw card number");
    }

    private static PaymentSucceededEvent succeeded(List<PaymentSucceededEvent.SettledFee> fees) {
        return new PaymentSucceededEvent(PAYMENT, GUARDIAN, new BigDecimal("10000.00"), "EGP",
                PaymentMethod.CREDIT_CARD, "411111******1111", NOW, fees);
    }
}
