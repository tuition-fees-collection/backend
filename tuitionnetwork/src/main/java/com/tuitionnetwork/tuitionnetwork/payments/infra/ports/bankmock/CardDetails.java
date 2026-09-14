package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardDetails(
        String number,
        @JsonProperty("holder_name") String holderName,
        @JsonProperty("expiry_month") Integer expiryMonth,
        @JsonProperty("expiry_year") Integer expiryYear,
        String cvv) {
}
