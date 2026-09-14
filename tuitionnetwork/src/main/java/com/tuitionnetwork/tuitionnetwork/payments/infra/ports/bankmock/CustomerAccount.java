package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

// ACTIVE | DORMANT | FROZEN
public record CustomerAccount(
        @JsonProperty("account_id") String accountId,
        @JsonProperty("account_number") String accountNumber,
        String type,
        String currency,
        @JsonProperty("available_balance") BigDecimal availableBalance,
        String status) {
}
