package com.tuitionnetwork.tuitionnetwork.payment.port.in;
import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
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
            String message,
            ProviderDetail provider
    ) {
        public PaymentResultView(UUID paymentId, PaymentStatus status, boolean requires3ds,
                                 String maskedCardNumber, String receiptReference,
                                 String message) {
            this(paymentId, status, requires3ds, maskedCardNumber, receiptReference,
                    message, null);
        }

        public boolean isPending() {
            return status.isInFlight() || status == PaymentStatus.RECON_REQUIRED;
        }
    }
    record ProviderDetail(
            @JsonProperty("payment_id") String paymentId,
            String status,
            Boolean approved,
            BigDecimal amount,
            @JsonProperty("captured_amount") BigDecimal capturedAmount,
            String reference,
            @JsonProperty("customer_id") String customerId,
            @JsonProperty("source_id") String sourceId,
            Source source,
            @JsonProperty("auth_code") String authCode,
            String rrn,
            @JsonProperty("response_code") String responseCode,
            @JsonProperty("response_message") String responseMessage
    ) {
        public record Source(
                String type,
                @JsonProperty("product_type") String productType,
                @JsonProperty("masked_number") String maskedNumber,
                String scheme
        ) {
        }
    }
}