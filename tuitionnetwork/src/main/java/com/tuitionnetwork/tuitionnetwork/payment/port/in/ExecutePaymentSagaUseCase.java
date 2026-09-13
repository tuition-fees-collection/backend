package com.tuitionnetwork.tuitionnetwork.payment.port.in;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
public interface ExecutePaymentSagaUseCase {
    PaymentResultView execute(ExecutePaymentCommand command);
    PaymentResultView confirm3ds(Confirm3dsCommand command);
    record ExecutePaymentCommand(
            UUID paymentId,
            UUID guardianId,
            String cardNumber,
            String cardHolderName,
            int expiryMonth,
            int expiryYear,
            String cvv
    ) {
        @Override
        public String toString() {
            return "ExecutePaymentCommand[paymentId=" + paymentId
                    + ", guardianId=" + guardianId + ", card=***REDACTED***]";
        }
    }
    record Confirm3dsCommand(UUID paymentId, UUID guardianId, String otp) {
        @Override
        public String toString() {
            return "Confirm3dsCommand[paymentId=" + paymentId + ", otp=***REDACTED***]";
        }
    }
    record PaymentResultView(
            UUID paymentId,
            PaymentStatus status,
            boolean requires3ds,
            String maskedCardNumber,
            String receiptReference,
            String message
    ) {
        public boolean isPending() {
            return status.isInFlight() || status == PaymentStatus.RECON_REQUIRED;
        }
    }
}
