package com.tuitionnetwork.tuitionnetwork.billing.api.dto;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;

import java.math.BigDecimal;
import java.util.UUID;


public record InstitutionCollectionResponse(
    UUID feeId,
    UUID studentId,
    String studentName,
    FeeType feeType,
    String collectionPeriod,
    BigDecimal amount,
    BigDecimal amountPaid,
    BigDecimal outstandingAmount,
    FeeStatus feeStatus
) {
    public static InstitutionCollectionResponse from(Fee fee){
        return from(fee, null);
    }

    public static InstitutionCollectionResponse from(Fee fee, String studentName){
        return new  InstitutionCollectionResponse(
            fee.getId(),
            fee.getStudentId(),
            studentName,
            fee.getFeeType(),
            fee.getCollectionPeriod(),
            fee.getAmount(),
            fee.getAmountPaid(),
            fee.getOutstandingAmount(),
            fee.getStatus()
        );
    }
    
}
