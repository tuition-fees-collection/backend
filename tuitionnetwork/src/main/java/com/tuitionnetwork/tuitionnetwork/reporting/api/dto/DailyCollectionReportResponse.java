package com.tuitionnetwork.tuitionnetwork.reporting.api.dto;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DailyCollectionReportResponse(
    LocalDate date,
    UUID institutionId,
    BigDecimal totalCollected,
    long paymentCount,
    Map<FeeType,BigDecimal> breakdownByFeeType,
    Map<PaymentMethod,BigDecimal> breakdownByPaymentMethod,
    List<DailyCollectionItemResponse> items
) {
    public static DailyCollectionReportResponse from(
            UUID institutionId,
            DailyCollectionReport report
    ) {
        return new DailyCollectionReportResponse(
                report.date(),
                institutionId,
                report.totalCollected(),
                report.paymentCount(),
                report.amountByFeeType(),
                report.amountByPaymentMethod(),
                report.items()
                        .stream()
                        .map(DailyCollectionItemResponse::from)
                        .toList()
        );
    }
}
