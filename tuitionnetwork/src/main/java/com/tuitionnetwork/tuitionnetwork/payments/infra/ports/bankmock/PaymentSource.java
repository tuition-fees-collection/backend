package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

// type: ACCOUNT | CARD , product_type: CURRENT | SAVINGS | CREDIT | DEBIT
public record PaymentSource(
        String type,
        @JsonProperty("product_type") String productType,
        @JsonProperty("account_number") String accountNumber,
        @JsonProperty("masked_number") String maskedNumber,
        String scheme) {
}
