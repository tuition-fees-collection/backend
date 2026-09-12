package com.tuitionnetwork.tuitionnetwork.identity.infra.ports.bankmock;

import java.math.BigDecimal;

public record MoiNameMatchResponse(
        String provided,
        Boolean matched,
        BigDecimal score,
        String note) {
}
