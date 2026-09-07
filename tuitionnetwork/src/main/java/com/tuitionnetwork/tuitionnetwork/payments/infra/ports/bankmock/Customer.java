package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Customer(
        @JsonProperty("national_id") String nationalId,
        @JsonProperty("full_name") String fullName,
        String email,
        String mobile) {
}
