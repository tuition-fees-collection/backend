package com.tuitionnetwork.tuitionnetwork.billing.api.dto;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;


import java.util.UUID;
import java.math.BigDecimal;

public record FeeSearchResponse(
    UUID id,
    UUID studentId,
    UUID institutionId,
    FeeType feeType,
    String collectionPeriod,   
    String currency,
    BigDecimal amount,
    BigDecimal amountPaid,
    FeeStatus status,
    BigDecimal outstandingAmount
){
    public static FeeSearchResponse from(Fee fee){
        return new FeeSearchResponse(
            fee.getId(),
            fee.getStudentId(),
            fee.getInstitutionId(),
            fee.getFeeType(),
            fee.getCollectionPeriod(),
            fee.getCurrency(),
            fee.getAmount(),
            fee.getAmountPaid(),
            fee.getStatus(),
            fee.getOutstandingAmount()
        );
    }

}