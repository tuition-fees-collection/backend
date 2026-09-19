package com.tuitionnetwork.tuitionnetwork.reporting.api.dto;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;


public record DailyCollectionItemResponse(
    UUID studentId,
    String studentName,
    FeeType feeType,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    Instant settledAt,
    String paymentReference
) {
    public static DailyCollectionItemResponse from(
            DailyCollectionRecord record
    ) {
        return from(record, null);
    }

    public static DailyCollectionItemResponse from(
            DailyCollectionRecord record,
            String studentName
    ) {
        return new DailyCollectionItemResponse(
                record.studentId(),
                studentName,
                record.feeType(),
                record.amountApplied(),
                record.paymentMethod(),
                record.settledAt(),
                record.paymentReference()
        );
    }
    
}
