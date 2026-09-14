package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;


public record CardPaymentRequest(
        CardDetails card,
        Amount amount,
        @JsonProperty("order_reference") String orderReference,
        Customer customer,
        String description,
        Boolean capture) {
}
