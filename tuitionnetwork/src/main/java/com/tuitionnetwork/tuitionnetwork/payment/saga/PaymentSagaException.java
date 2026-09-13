package com.tuitionnetwork.tuitionnetwork.payment.saga;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentSagaStepType;
public class PaymentSagaException extends RuntimeException {
    private final UUID paymentId;
    private final PaymentSagaStepType step;
    public PaymentSagaException(UUID paymentId, PaymentSagaStepType step, String message, Throwable cause) {
        super(message, cause);
        this.paymentId = paymentId;
        this.step = step;
    }
    public UUID getPaymentId() {
        return paymentId;
    }
    public PaymentSagaStepType getStep() {
        return step;
    }
}