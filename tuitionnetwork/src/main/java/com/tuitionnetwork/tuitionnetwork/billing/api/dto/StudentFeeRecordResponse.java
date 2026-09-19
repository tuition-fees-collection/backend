package com.tuitionnetwork.tuitionnetwork.billing.api.dto;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentFeeRecordResponse (
    UUID studentId,
    String studentName,
    String institutionName,
    BigDecimal yearTotal,
    BigDecimal amountPaid,
    BigDecimal amountOutstanding,
    List<FeeSearchResponse> fees
){
    public static StudentFeeRecordResponse from(
        UUID studentId,
        List<Fee> fees
    ){
        return from(studentId, null, null, fees);
    }

    public static StudentFeeRecordResponse from(
        UUID studentId,
        String studentName,
        String institutionName,
        List<Fee> fees
    ){
        BigDecimal yearTotal=fees.stream().
        map(fee->fee.getAmount()).
        reduce(BigDecimal.ZERO,(total,amount)->total.add(amount));

        BigDecimal amountPaid=fees.stream().
        map(fee->fee.getAmountPaid()).
        reduce(BigDecimal.ZERO,(total,amount)->total.add(amount));

        BigDecimal amountOutstanding=fees.stream().
        map(fee->fee.getOutstandingAmount()).
        reduce(BigDecimal.ZERO,(total,amount)->total.add(amount));

        List<FeeSearchResponse> feeItems=fees.stream().
        map(FeeSearchResponse::from).
        toList();

        return new StudentFeeRecordResponse(
            studentId, studentName, institutionName, yearTotal, amountPaid, amountOutstanding, feeItems
        );
    }
}
