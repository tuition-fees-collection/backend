package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.math.BigDecimal;
import java.util.UUID;

public interface CardgatewayPort {
    Result charge(ChargeCommand command, String idempotencyKey);
    Result confirm3ds(String gatewayPaymentId, String otp);
    Result getStatus(String gatewayPaymentId);
    enum Outcome {
        APPROVED,
        REQUIRES_3DS,
        DECLINED,
        REJECTED,
        UNKNOWN,
        NOT_FOUND;
        public boolean isDefiniteFailure() {
            return this == DECLINED || this == REJECTED;
        }
        public boolean isRetryable() {
            return this == UNKNOWN;
        }
        public boolean needsReconciliation() {
            return this == UNKNOWN || this == NOT_FOUND;
        }
    }

    
    record ChargeCommand(
            UUID paymentId,
            String cardNumber,
            String cardHolderName,
            int expiryMonth,
            int expiryYear,
            String cvv,
            BigDecimal amount,
            String currency,
            String description,
            boolean capture
    ) {
        public ChargeCommand {
            if (amount == null || amount.signum() <= 0) {
                throw new IllegalArgumentException("Charge amount must be greater than zero");
            }
        }

    
        @Override
        public String toString() {
            return "ChargeCommand[paymentId=" + paymentId
                    + ", amount=" + amount + " " + currency
                    + ", card=***REDACTED***]";
        }
    }

    record Result(
            Outcome outcome,
            String gatewayPaymentId,
            String gatewayStatus,
            BigDecimal capturedAmount,
            String authCode,
            String rrn,
            String responseCode,
            String responseMessage,
            String maskedCardNumber,
            String cardScheme,
            String cardType,
            String errorCode
    ) {
        public static Result unknown(String gatewayPaymentId, String reason) {
            return new Result(Outcome.UNKNOWN, gatewayPaymentId, null, null, null, null,
                    null, reason, null, null, null, null);
        }

        public static Result notFound(String gatewayPaymentId) {
            return new Result(Outcome.NOT_FOUND, gatewayPaymentId, null, null, null, null,
                    null, "No record at provider", null, null, null, null);
        }

        public boolean isApproved() {
            return outcome == Outcome.APPROVED;
        }

        public boolean requires3ds() {
            return outcome == Outcome.REQUIRES_3DS;
        }

       
        public boolean isCreditCard() {
            return "CREDIT".equalsIgnoreCase(cardType);
        }
    }
}