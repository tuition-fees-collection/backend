package com.tuitionnetwork.tuitionnetwork.reporting.domain;

import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import com.tuitionnetwork.tuitionnetwork.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DailyCollectionRecord(
    UUID paymentId,
    UUID studentId,
    FeeType feeType,
    BigDecimal amountApplied,
    PaymentMethod paymentMethod,
    Instant settledAt,
    String paymentReference

){

}