package com.tuitionnetwork.tuitionnetwork.billing.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;




@Service
@Transactional

public class BillingService{
    private final FeeService feeService;

    public BillingService(FeeService feeService){
        this.feeService=feeService;
    }

   
    public Fee getPayableFee(UUID feeId){
        return feeService.getPayableFee(feeId);
    }

    
     public BigDecimal getOutstandingAmount(UUID feeId){
        return feeService.getOutstandingAmount(feeId);
     }

     /*settle a fee, rejecting the call if the fee has moved since
       expectedVersion was read (stale caller or lost race) */
     public Fee settle(UUID feeId,BigDecimal amount,int expectedVersion){
        return feeService.settle(feeId,amount,expectedVersion);
     }

    
     @Transactional (readOnly = true)
     public List<Fee> findAllById(List<UUID> feeIds){
        return feeService.findAllById(feeIds);
     }


 


    
}