package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

// AUTHORISED | CAPTURED | PENDING_3DS | DECLINED 

public record CardPaymentResponse(
        @JsonProperty("payment_id") String paymentId,
        String status,
        Boolean approved,
        BigDecimal amount,
        String currency,
        @JsonProperty("captured_amount") BigDecimal capturedAmount,
        @JsonProperty("order_reference") String orderReference,
        CardSummary card,
        @JsonProperty("auth_code") String authCode,
        String rrn,
        @JsonProperty("response_code") String responseCode,
        @JsonProperty("response_message") String responseMessage,
        @JsonProperty("three_ds") Map<String, Object> threeDs,
        @JsonProperty("epp_plan_id") String eppPlanId,
        Map<String, Object> customer,
        String description,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt) {
}
