package com.tuitionnetwork.tuitionnetwork.payment.port.in;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
public interface ExecutePaymentSagaUseCase {
    PaymentResultView execute(ExecutePaymentCommand command);
    PaymentResultView confirm3ds(Confirm3dsCommand command);
    record ExecutePaymentCommand(
            UUID paymentId,
            String cardNumber,
            String cardHolderName,
            int expiryMonth,
            int expiryYear,
            String cvv,
            String sourceId,
            String nationalId
    ) {
        @Override
        public String toString() {
            return "ExecutePaymentCommand[paymentId=" + paymentId + ", card=***REDACTED***]";
        }
        public static ExecutePaymentCommand withCard(UUID paymentId, String cardNumber,
                                                     String cardHolderName, int expiryMonth,
                                                     int expiryYear, String cvv) {
            return new ExecutePaymentCommand(paymentId, cardNumber, cardHolderName,
                    expiryMonth, expiryYear, cvv, null, null);
        }
        public static ExecutePaymentCommand forCash(UUID paymentId) {
            return new ExecutePaymentCommand(paymentId, null, null, 0, 0, null, null, null);
        }
        public static ExecutePaymentCommand withSource(UUID paymentId, String sourceId,
                                                       String nationalId) {
            return new ExecutePaymentCommand(paymentId, null, null, 0, 0, null,
                    sourceId, nationalId);
        }

        public boolean hasCard() {
            return cardNumber != null && !cardNumber.isBlank();
        }

        public boolean hasSource() {
            return sourceId != null && !sourceId.isBlank();
        }
    }
    record Confirm3dsCommand(UUID paymentId, String otp) {
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