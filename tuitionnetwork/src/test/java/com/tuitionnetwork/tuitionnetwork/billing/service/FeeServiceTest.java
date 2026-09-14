package com.tuitionnetwork.tuitionnetwork.billing.service;

import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.infra.FeeRepository;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

 class FeeServiceTest {
    @Mock 
    private FeeRepository feeRepository;
    private FeeService feeService;
    private UUID feeId;
    private UUID studentId;
    private UUID institutionId;
    private Fee fee;

    @BeforeEach 
    void settUp(){
        feeService=new FeeService(feeRepository);
        feeId=UUID.randomUUID();
        studentId=UUID.randomUUID();
        institutionId=UUID.randomUUID();
        fee=new Fee(
            studentId,
            institutionId,
            FeeType.TUITION,
            "2026-2027",
            "EGP",
            new BigDecimal("10000.00"),
      null
        );
    }
         @Test 
         /*get fee should return fee when fee exist */
         void testGetFee_Success(){
            /*arrange */
            when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
            /*act */
            Fee result=feeService.getFee(feeId);
            /*assert */
            assertNotNull(result);
            assertEquals(fee,result);
            verify(feeRepository).findById(feeId);

         }

         @Test 
         /*get fee should throw exception when fee not found */
         void testgetFee_FeeNotFound(){
            when(feeRepository.findById(feeId)).thenReturn(Optional.empty());
            IllegalArgumentException exception=
            assertThrows(IllegalArgumentException.class,()->{
                feeService.getFee(feeId);
            });

            assertTrue(exception.getMessage().contains("Fee not found"));

         }

         @Test 
         /*get payable fee should return fee when fee is payable */
         void testGetPayableFee_Success(){
            when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
            Fee result=feeService.getPayableFee(feeId);

            
            assertEquals(fee,result);
            assertEquals(FeeStatus.OUTSTANDING,fee.getStatus());

         }

         @Test 
            /*get payable fee should throw exception when fee is already paid */
            void testGetPayableFee_FeeAlreadyPaid(){

               fee.applyPayment(new BigDecimal("10000.00"));
               when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));

               IllegalArgumentException exception=
                assertThrows(IllegalArgumentException.class,()->{
                      feeService.getPayableFee(feeId);
                });

                assertTrue(exception.getMessage().contains("Fee is already paid"));
            }

            @Test 
            void testGetOutstandingAmount_Success(){
                fee.applyPayment(new BigDecimal("3000.00"));
                when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
                BigDecimal result=feeService.getOutstandingAmount(feeId);
                assertEquals(new BigDecimal("7000.00"),result);
            }

            @Test 
            /*validate amount should pass when the amount is valid */
            void testValidateAmount_Success(){
                when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
                assertDoesNotThrow(()->{
                    feeService.validatePaymentAmount(feeId,new BigDecimal("5000.00"));
                });
            }

            @Test 
            /*validate amount should throw exception when the amount is zero */
            void testValidateAmount_WhenAmountIsZero(){
                when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
               
                assertThrows(IllegalArgumentException.class,()->{
                    feeService.validatePaymentAmount(feeId, BigDecimal.ZERO);
                });

               
            }

            @Test 
            /*validate amount should throw exception when the amount exceed outstanding */
            void testValidateAmount_WhenAmountExceedOutstanding(){

                when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
                
                assertThrows(IllegalArgumentException.class,()->{
                    feeService.validatePaymentAmount(feeId,new BigDecimal("12000.00"));
                });

                
            }

    
}
