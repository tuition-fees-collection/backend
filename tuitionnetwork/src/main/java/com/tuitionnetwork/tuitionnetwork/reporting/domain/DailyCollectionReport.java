package com.tuitionnetwork.tuitionnetwork.reporting.domain;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DailyCollectionReport(
        LocalDate date,
        BigDecimal totalCollected,
        long paymentCount,
        Map<FeeType, BigDecimal> amountByFeeType,
        Map<PaymentMethod, BigDecimal> amountByPaymentMethod,
        List<DailyCollectionRecord> items
) {
}