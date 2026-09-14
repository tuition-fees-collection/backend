package com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** status: DUE | PAID | CANCELLED */
public record EppInstallment(
        Integer number,
        @JsonProperty("due_date") String dueDate,
        BigDecimal amount,
        String status) {
}
