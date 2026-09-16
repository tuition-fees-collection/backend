package com.tuitionnetwork.tuitionnetwork.billing.domain;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



import java.util.UUID;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

 class FeeTest {
    private Fee fee;

    @BeforeEach
    void setUp(){
       fee= new Fee(
        UUID.randomUUID(),
        UUID.randomUUID(),
        FeeType.TUITION,
        "2026-2027",
        "EGP",
        new BigDecimal("10000.00"),
        null
       );
       

    }

    @Test 
    void testApplyPayment_PartialPayment(){
        fee.applyPayment(new BigDecimal("3000.00"));

        assertEquals(new BigDecimal("3000.00"),fee.getAmountPaid());
        assertEquals(new BigDecimal("7000.00"),fee.getOutstandingAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID,fee.getStatus());
    }

    @Test 
    void testApplyPayment_MultiplePartialPayments(){
        fee.applyPayment(new BigDecimal("3000.00"));
        fee.applyPayment(new BigDecimal("2000.00"));

        assertEquals(new BigDecimal("5000.00"),fee.getAmountPaid());
        assertEquals(new BigDecimal("5000.00"),fee.getOutstandingAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID,fee.getStatus());
    }

    @Test 
    void testApplyPayment_FullPayment(){
        fee.applyPayment(new BigDecimal("10000.00"));

        assertEquals(new BigDecimal("10000.00"),fee.getAmountPaid());
        assertEquals(new BigDecimal("0.00"),fee.getOutstandingAmount());
        assertEquals(FeeStatus.SETTLED,fee.getStatus());
    }

    @Test 
    /*should throw exception */
    void testApplyPaymenr_ZeroAmount(){
        
        assertThrows(IllegalArgumentException.class,()->
                  fee.applyPayment(new BigDecimal("0.00")));

    }

    @Test 
    /*should throw exception */
    void testApplyPayment_NegativeAmount(){
        
        assertThrows(IllegalArgumentException.class,()->
                  fee.applyPayment(new BigDecimal("-100.00")));

    }

    @Test 
    /*should throw exception */
    void testApplyPayment_ExceedingOutstandingAmount(){
        
        assertThrows(IllegalArgumentException.class,()->
                  fee.applyPayment(new BigDecimal("12000.00")));

    }

   
    
}
