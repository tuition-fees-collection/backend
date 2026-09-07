package com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record EppQuoteOption(
        @JsonProperty("tenor_months") Integer tenorMonths,
        @JsonProperty("annual_rate") BigDecimal annualRate,
        @JsonProperty("interest_amount") BigDecimal interestAmount,
        @JsonProperty("admin_fee") BigDecimal adminFee,
        @JsonProperty("total_payable") BigDecimal totalPayable,
        @JsonProperty("monthly_installment") BigDecimal monthlyInstallment) {
}
