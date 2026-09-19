package com.tuitionnetwork.tuitionnetwork.billing.api.dto;

import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentReceiptResponse(
        UUID paymentId,
        String referenceNumber,
        Instant issuedAt,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        String studentName,
        String institutionName,
        List<Line> lines
) {
    public record Line(
            UUID feeId,
            String feeType,
            String collectionPeriod,
            BigDecimal amountApplied
    ) {
    }

    public StudentReceiptResponse withNames(String studentName, String institutionName) {
        return new StudentReceiptResponse(
                paymentId,
                referenceNumber,
                issuedAt,
                amount,
                currency,
                method,
                studentName,
                institutionName,
                lines
        );
    }
}
