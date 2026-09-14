package com.tuitionnetwork.tuitionnetwork.identity.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MoiValidateRequest(
        @JsonProperty("national_id") String nationalId,
        @JsonProperty("full_name") String fullName,
        String purpose) {
}
