package com.tuitionnetwork.tuitionnetwork.billing.api.dto;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StudentReceiptLine(
        UUID paymentId,
        String referenceNumber,
        Instant issuedAt,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        UUID feeId,
        FeeType feeType,
        String collectionPeriod,
        BigDecimal amountApplied
) {
}
