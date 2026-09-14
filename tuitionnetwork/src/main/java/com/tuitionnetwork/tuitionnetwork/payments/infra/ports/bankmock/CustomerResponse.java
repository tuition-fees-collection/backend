package com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CustomerResponse(
        @JsonProperty("customer_id") String customerId,
        @JsonProperty("national_id") String nationalId,
        @JsonProperty("full_name_en") String fullNameEnglish,
        @JsonProperty("full_name_ar") String fullNameArabic,
        String mobile,
        String status,
        List<CustomerAccount> accounts,
        List<CustomerCard> cards) {
}
