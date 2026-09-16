package com.tuitionnetwork.tuitionnetwork.reporting.api.dto;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;


public record DailyCollectionItemResponse(
    UUID studentId,
    FeeType feeType,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    Instant settledAt,
    String paymentReference
) {
    public static DailyCollectionItemResponse from(
            DailyCollectionRecord record
    ) {
        return new DailyCollectionItemResponse(
                record.studentId(),
                record.feeType(),
                record.amountApplied(),
                record.paymentMethod(),
                record.settledAt(),
                record.paymentReference()
        );
    }
    
}
