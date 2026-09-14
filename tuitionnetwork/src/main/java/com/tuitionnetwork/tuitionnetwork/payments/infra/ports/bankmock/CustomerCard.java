package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

// ACTIVE | BLOCKED | EXPIRED
public record CustomerCard(
        @JsonProperty("card_id") String cardId,
        @JsonProperty("masked_number") String maskedNumber,
        String scheme,
        String type,
        @JsonProperty("holder_name") String holderName,
        String expiry,
        String status,
        @JsonProperty("credit_limit") BigDecimal creditLimit,
        @JsonProperty("available_limit") BigDecimal availableLimit,
        @JsonProperty("linked_account_id") String linkedAccountId) {
}
