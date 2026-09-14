package com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock;

import java.math.BigDecimal;
import java.util.List;

public record EppQuoteResponse(
        BigDecimal amount,
        String currency,
        List<EppQuoteOption> options) {
}
