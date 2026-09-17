package com.tuitionnetwork.tuitionnetwork.payment.port.in;
import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentStatus;
public interface ExecutePaymentSagaUseCase {
    PaymentResultView execute(ExecutePaymentCommand command);
    PaymentResultView confirm3ds(Confirm3dsCommand command);
    /**
     * One attempt to take the money.
     *
     * Three shapes share this record, because the saga runs the same steps
     * whichever it is and only the handler differs:
     *
     *   card     cardNumber..cvv set             POST /{id}/pay
     *   cash     everything null                 POST /{id}/pay-cash
     *   source   sourceId and nationalId set     POST /{id}/pay-source
     */
    record ExecutePaymentCommand(
            UUID paymentId,
            String cardNumber,
            String cardHolderName,
            int expiryMonth,
            int expiryYear,
            String cvv,

            /** An account or card the bank already holds. Opaque, not a credential. */
            String sourceId,

            /**
             * The national ID the officer typed into the lookup, carried so the
             * source can be checked against it before anything is charged. The
             * provider does not check: it resolves the customer FROM the source
             * id, so a source id alone is enough to move money.
             */
            String nationalId
    ) {

        /** Card details. Redacted - a record prints every field by default. */
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

            /** What the bank said, verbatim. Null for cash, which has no provider. */
            ProviderDetail provider
    ) {

        /** The shape callers had before provider detail existed. */
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

    /**
     * The provider's own answer, in the provider's own words.
     *
     * Field names are the provider's - snake_case, payment_id rather than
     * paymentId - deliberately. Anyone reading this is comparing it against the
     * bank's records or its documentation, and translating the names would make
     * that harder for no gain.
     *
     * Returned as the whole body by /pay-source, because an officer at a counter
     * reconciles against the bank's record. The receipt number is not here; it is
     * on GET /{paymentId}/receipt, which is where a parent's copy comes from.
     */
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

        /** Where the money came from, as the provider describes it. */
        public record Source(
                String type,
                @JsonProperty("product_type") String productType,
                @JsonProperty("masked_number") String maskedNumber,
                String scheme
        ) {
        }
    }
}