package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

// POSTED | AUTHORISED | CAPTURED | DECLINED | VOIDED | REFUNDED | PARTIALLY_REFUNDED
public record BackofficePaymentResponse(
        @JsonProperty("payment_id") String paymentId,
        String status,
        Boolean approved,
        BigDecimal amount,
        String currency,
        @JsonProperty("captured_amount") BigDecimal capturedAmount,
        @JsonProperty("refunded_amount") BigDecimal refundedAmount,
        String reference,
        String description,
        @JsonProperty("customer_id") String customerId,
        @JsonProperty("source_id") String sourceId,
        PaymentSource source,
        @JsonProperty("auth_code") String authCode,
        String rrn,
        @JsonProperty("response_code") String responseCode,
        @JsonProperty("response_message") String responseMessage,
        @JsonProperty("epp_plan_id") String eppPlanId,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt) {
}
