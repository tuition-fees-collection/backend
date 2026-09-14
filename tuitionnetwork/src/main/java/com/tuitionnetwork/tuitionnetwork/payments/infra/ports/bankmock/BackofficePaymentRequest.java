package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BackofficePaymentRequest(
        @JsonProperty("source_id") String sourceId,
        Amount amount,
        String reference,
        String description,
        Boolean capture) {
}
