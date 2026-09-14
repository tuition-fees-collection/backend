package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import java.math.BigDecimal;

public record Amount(
        BigDecimal value,
        String currency) {
}
