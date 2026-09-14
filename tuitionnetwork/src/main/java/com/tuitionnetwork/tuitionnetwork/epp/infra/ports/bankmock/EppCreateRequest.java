package com.tuitionnetwork.tuitionnetwork.epp.infra.ports.bankmock;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.Amount;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.CardDetails;
import com.tuitionnetwork.tuitionnetwork.payments.infra.ports.bankmock.Customer;


public record EppCreateRequest(
        @JsonProperty("tenor_months") Integer tenorMonths,
        @JsonProperty("payment_id") String paymentId,
        CardDetails card,
        Amount amount,
        Customer customer,
        @JsonProperty("product_name") String productName) {
}
