package com.tuitionnetwork.tuitionnetwork.billing.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;


@Service 
public class FeeBalanceService{


    
    public BigDecimal getOutstandingAmount(Fee fee){
        return fee.getOutstandingAmount();
    }


    
    public BigDecimal getTotalOutstandingAmount(List<Fee> fees){
        return fees.stream()
        .map(fee->fee.getOutstandingAmount())
        .reduce(BigDecimal.ZERO,(total,amount)->total.add(amount));
    }

    
}