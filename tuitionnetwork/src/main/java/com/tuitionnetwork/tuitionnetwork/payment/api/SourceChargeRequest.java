package com.tuitionnetwork.tuitionnetwork.payment.api;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
public record SourceChargeRequest(
        @JsonProperty("source_id") @JsonAlias("sourceId") String sourceId,
        @JsonProperty("national_id") @JsonAlias("nationalId") String nationalId,
        Amount amount,
        String reference,
        String description,
        boolean capture
) {
    public record Amount(
            BigDecimal value,
            String currency
    ) {}
}
