package com.tuitionnetwork.tuitionnetwork.billing.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.math.BigDecimal;



@Service
@Transactional

public class BillingService{
    private final FeeService feeService;

    public BillingService(FeeService feeService){
        this.feeService=feeService;
    }

    /*get payable fees only (Read/Check*/
    public Fee getPayableFee(UUID feeId){
        return feeService.getPayableFee(feeId);
    }

     /*get outstanding amount fee for a fee */
     public BigDecimal getOutstandingAmount(UUID feeId){
        return feeService.getOutstandingAmount(feeId);
     }

     /*get payable fees and lock it at payment processing */
     public Fee getPayableFeeForUpdate(UUID feeId){
        return feeService.getPayableFeeForUpdate(feeId);
     }


    
}