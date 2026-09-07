package com.tuitionnetwork.tuitionnetwork.epp.web;

import java.util.UUID;

public record CreateEppPlanRequest(
        UUID paymentId,
        String gatewayPaymentId,
        int tenorMonths,
        String productName) {
}
