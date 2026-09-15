package com.tuitionnetwork.tuitionnetwork.billing.api.dto;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentFeeRecordResponse (
    UUID studentId,
    BigDecimal yearTotal,
    BigDecimal amountPaid,
    BigDecimal amountOutstanding,
    List<FeeSearchResponse> fees
){
    public static StudentFeeRecordResponse from(
        UUID studentId,
        List<Fee> fees
    ){
        BigDecimal yearTotal=fees.stream().
        map(Fee::getAmount).
        reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal amountPaid=fees.stream().
        map(Fee::getAmountPaid).
        reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal amountOutstanding=fees.stream().
        map(Fee::getOutstandingAmount).
        reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FeeSearchResponse> feeItems=fees.stream().
        map(FeeSearchResponse::from).
        toList();

        return new StudentFeeRecordResponse(
            studentId, yearTotal, amountPaid, amountOutstanding, feeItems
        );
    }
}

    

