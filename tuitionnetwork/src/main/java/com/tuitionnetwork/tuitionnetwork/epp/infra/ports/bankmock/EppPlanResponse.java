package com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** status: ACTIVE | CANCELLED | COMPLETED */
public record EppPlanResponse(
        @JsonProperty("plan_id") String planId,
        String status,
        @JsonProperty("payment_id") String paymentId,
        BigDecimal principal,
        String currency,
        @JsonProperty("tenor_months") Integer tenorMonths,
        @JsonProperty("annual_rate") BigDecimal annualRate,
        @JsonProperty("interest_amount") BigDecimal interestAmount,
        @JsonProperty("admin_fee") BigDecimal adminFee,
        @JsonProperty("total_payable") BigDecimal totalPayable,
        @JsonProperty("monthly_installment") BigDecimal monthlyInstallment,
        @JsonProperty("first_due_date") String firstDueDate,
        @JsonProperty("last_due_date") String lastDueDate,
        List<EppInstallment> schedule,
        CardSummary card,
        Map<String, Object> customer,
        @JsonProperty("product_name") String productName,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt) {
}
