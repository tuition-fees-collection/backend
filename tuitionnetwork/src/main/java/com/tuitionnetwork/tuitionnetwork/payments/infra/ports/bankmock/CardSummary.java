package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CardSummary(
        @JsonProperty("masked_number") String maskedNumber,
        String scheme,
        String type,
        @JsonProperty("holder_name") String holderName,
        String expiry) {
}
