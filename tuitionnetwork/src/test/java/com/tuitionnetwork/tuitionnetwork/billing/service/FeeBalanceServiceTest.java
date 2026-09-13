package com.tuitionnetwork.tuitionnetwork.billing.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;



 class FeeBalanceServiceTest {
    private FeeBalanceService feeBalanceService;
    private UUID studentId;
    private UUID institutionId;

    @BeforeEach 
    void setUp(){
        feeBalanceService=new FeeBalanceService();
        studentId=UUID.randomUUID();
        institutionId=UUID.randomUUID();
    }

    @Test 
    void testGetOutstandingAmount(){
       Fee fee=new Fee(
        studentId,
        institutionId,
        FeeType.TUITION,
        "2026-2027",
        "EGP",
        new BigDecimal("10000.00"),
        null
       );

       fee.applyPayment(new BigDecimal("3000.00"));
       BigDecimal result=feeBalanceService.getOutstandingAmount(fee);
       assertEquals(new BigDecimal("7000.00"), result);
    }

    @Test 
    void testGetTotalOutstandingAmount(){
        Fee fee=new Fee(    
        studentId,
        institutionId,
        FeeType.TUITION,
        "2026-2027",
        "EGP",
        new BigDecimal("10000.00"),
        null
        );

        Fee fee2=new Fee(
            studentId,
            institutionId,
            FeeType.BUS,
            "2026-2027",
            "EGP",
            new BigDecimal("3000.00"),
            null
        );

        fee.applyPayment(new BigDecimal("2000.00"));
        fee2.applyPayment(new BigDecimal("1000.00"));
       List<Fee> fees=List.of(fee,fee2);
       BigDecimal result=feeBalanceService.getTotalOutstandingAmount(fees);
       assertEquals(new BigDecimal("10000.00"), result);
    }

    @Test 
    void testGetTotalOutstandingAmount_EmptyList(){
        BigDecimal result=feeBalanceService.getTotalOutstandingAmount(List.of());
        assertEquals(BigDecimal.ZERO, result);
        
    }

    
}
