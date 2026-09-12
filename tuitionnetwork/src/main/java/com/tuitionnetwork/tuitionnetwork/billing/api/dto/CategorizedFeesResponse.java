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
    Map<FeeType,List<FeeSearchResponse>> feesByType,
    BigDecimal totalOutstandingAmount

){
    public static CategorizedFeesResponse from(UUID studentId, List<Fee> fees){
        Map<FeeType,List<FeeSearchResponse>> feesByType=fees.stream()
            .collect(Collectors.groupingBy(
                Fee::getFeeType,
                Collectors.mapping(FeeSearchResponse::from,Collectors.toList())
            ));
        BigDecimal totalOutstandingAmount=fees.stream()
            .map(Fee::getOutstandingAmount)
            .reduce(BigDecimal.ZERO,BigDecimal::add);
        return new CategorizedFeesResponse(studentId,feesByType,totalOutstandingAmount);
    }
   

}
    

