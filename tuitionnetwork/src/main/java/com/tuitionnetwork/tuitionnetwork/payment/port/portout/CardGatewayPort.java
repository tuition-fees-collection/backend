package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.math.BigDecimal;
import java.util.UUID;

public interface CardGatewayPort {
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
            String errorCode,

            /*
             * The four below are the provider's own view of the payment, carried
             * so a response can show what the bank recorded rather than only what
             * we concluded from it. An officer at a counter reconciles against
             * these, not against our status.
             *
             * Null on the card path: /payments/cards resolves no customer and
             * echoes no reference, so there is nothing honest to put here.
             */
            String customerId,
            String sourceId,
            String reference,
            BigDecimal amount
    ) {

        /**
         * The shape callers had before provider detail was carried.
         *
         * Kept so every existing construction - the card bridge, the contract
         * tests - compiles unchanged. A result built this way simply has no
         * provider-side detail, which is true of the card path anyway.
         */
        public Result(Outcome outcome, String gatewayPaymentId, String gatewayStatus,
                      BigDecimal capturedAmount, String authCode, String rrn,
                      String responseCode, String responseMessage, String maskedCardNumber,
                      String cardScheme, String cardType, String errorCode) {
            this(outcome, gatewayPaymentId, gatewayStatus, capturedAmount, authCode, rrn,
                    responseCode, responseMessage, maskedCardNumber, cardScheme, cardType,
                    errorCode, null, null, null, null);
        }

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