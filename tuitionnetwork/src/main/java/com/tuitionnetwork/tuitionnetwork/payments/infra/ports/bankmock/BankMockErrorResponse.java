package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankMockErrorResponse(BankMockError error, @JsonProperty("request_id") String requestId) {
}
