package com.tuitionnetwork.tuitionnetwork.billing.api.dto;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.stream.Collectors;


public record CategorizedFeesResponse(
    UUID studentId,
    String studentName,
    String institutionName,
    Map<FeeType,List<FeeSearchResponse>> feesByType,
    BigDecimal totalOutstandingAmount

){
    public static CategorizedFeesResponse from(UUID studentId, List<Fee> fees){
        return from(studentId, null, null, fees);
    }

    public static CategorizedFeesResponse from(
            UUID studentId,
            String studentName,
            String institutionName,
            List<Fee> fees
    ){
        Map<FeeType,List<FeeSearchResponse>> feesByType=fees.stream()
            .collect(Collectors.groupingBy(
                fee->fee.getFeeType(),
                Collectors.mapping(FeeSearchResponse::from,Collectors.toList())
            ));
        BigDecimal totalOutstandingAmount=fees.stream()
            .map(fee->fee.getOutstandingAmount())
            .reduce(BigDecimal.ZERO,(total,amount)->total.add(amount));
        return new CategorizedFeesResponse(studentId, studentName, institutionName, feesByType, totalOutstandingAmount);
    }
}
