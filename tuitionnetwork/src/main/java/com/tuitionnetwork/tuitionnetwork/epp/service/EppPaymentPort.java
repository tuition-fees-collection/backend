package com.tuitionnetwork.tuitionnetwork.epp.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface EppPaymentPort {

    EligiblePayment requireEligiblePayment(UUID paymentId);

    record EligiblePayment(
            UUID paymentId,
            String gatewayPaymentId,
            BigDecimal amount,
            String currency) {
    }
}
