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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

            @Test
void getFeesForStudentAndInstitution_ReturnsFees() {

    UUID studentId = UUID.randomUUID();
    UUID institutionId = UUID.randomUUID();

    Fee fee = new Fee(
            studentId,
            institutionId,
            FeeType.TUITION,
            "2026-2027",
            "EGP",
            new BigDecimal("10000.00"),
            null
    );

    when(feeRepository.findByStudentIdAndInstitutionId(
            studentId,
            institutionId
    )).thenReturn(List.of(fee));

    List<Fee> result =
            feeService.getFeesForStudentAndInstitution(
                    studentId,
                    institutionId
            );

    assertEquals(1, result.size());
    assertSame(fee, result.get(0));

    verify(feeRepository).findByStudentIdAndInstitutionId(
            studentId,
            institutionId
    );
}

            @Test
            void getFeesForStudentAndInstitution_WhenNoFees_ThrowsException() {

                      UUID studentId = UUID.randomUUID();
                      UUID institutionId = UUID.randomUUID();

                      when(feeRepository.findByStudentIdAndInstitutionId(
                               studentId,
                               institutionId
                           )).thenReturn(List.of());

                      FeeNotFoundException exception = assertThrows(
                        FeeNotFoundException.class, () ->
                        feeService.getFeesForStudentAndInstitution(
                        studentId,
                        institutionId
                        )
                     );

                        assertEquals("Student fee record not found",exception.getMessage());

                        verify(feeRepository).findByStudentIdAndInstitutionId(studentId,institutionId);
             }

              @Test
            void testGetInstitutionCollections() {
                      UUID institutionId = UUID.randomUUID();

                      fee=new Fee(
                             studentId,
                            institutionId,
                             FeeType.TUITION,
                             "2026-2027",
                             "EGP",
                             new BigDecimal("10000.00"),
                             null
                         );

                      when(feeRepository.findInstitutionCollections(
                               institutionId,
                               FeeStatus.OUTSTANDING,
                               null,
                               null
                           )).thenReturn(List.of(fee));

                    List<Fee> result=feeService.getInstitutionCollections(
                        institutionId,
                         FeeStatus.OUTSTANDING, 
                         null, 
                         null
                        );

                        assertEquals(1, result.size());
                        assertEquals(fee, result.get(0));

                        verify(feeRepository).findInstitutionCollections(
                            institutionId,
                             FeeStatus.OUTSTANDING,
                              null,
                               null
                            );

                     
             }
             @Test
            void testGetInstitutionCollections_WithFilters() {
                      UUID institutionId = UUID.randomUUID();

                      fee=new Fee(
                             studentId,
                            institutionId,
                             FeeType.TUITION,
                             "2026-2027",
                             "EGP",
                             new BigDecimal("10000.00"),
                             null
                         );

                      when(feeRepository.findInstitutionCollections(
                               institutionId,
                               FeeStatus.OUTSTANDING,
                               FeeType.TUITION,
             "2026-2027"
                           )).thenReturn(List.of(fee));

                    List<Fee> result=feeService.getInstitutionCollections(
                        institutionId,
                         FeeStatus.OUTSTANDING, 
                         FeeType.TUITION, 
       "2026-2027"
                        );

                        assertEquals(1, result.size());
                        assertEquals(FeeType.TUITION, result.get(0).getFeeType());
                        assertEquals("2026-2027", result.get(0).getCollectionPeriod());

                        verify(feeRepository).findInstitutionCollections(
                            institutionId,
                             FeeStatus.OUTSTANDING,
                              FeeType.TUITION,
            "2026-2027"
                            );

                     
             }
              @Test
              void testGetInstitutionCollections_NullInstitutionId() {
                     IllegalArgumentException exception=assertThrows(
                        IllegalArgumentException.class,
                         ()->feeService.getInstitutionCollections(
                            null,
                             FeeStatus.OUTSTANDING,
                              null,
                               null));

                               assertEquals( "Institution ID is required", exception.getMessage());
                                verifyNoInteractions(feeRepository);
             }

              @Test
              void testGetInstitutionCollections_NullStatus() {
                     UUID institutionId=UUID.randomUUID();
                     IllegalArgumentException exception=assertThrows(
                        IllegalArgumentException.class,
                         ()->feeService.getInstitutionCollections(
                            institutionId,
                             null,
                              null,
                               null));

                               assertEquals( "Fee status is required", exception.getMessage());
                                verifyNoInteractions(feeRepository);
             }

             @Test
             void settle_PartialPayment_UpdatesAndSavesFee() {
                          when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
                          when(feeRepository.save(fee)).thenReturn(fee);
                          Fee result = feeService.settle(feeId,new BigDecimal("3000.00"));
                          assertSame(fee, result);
                          assertEquals(new BigDecimal("3000.00"),result.getAmountPaid());
                          assertEquals(new BigDecimal("7000.00"),result.getOutstandingAmount());
                          assertEquals(FeeStatus.PARTIALLY_PAID, result.getStatus());
                          verify(feeRepository).findById(feeId);
                          verify(feeRepository).save(fee);
                        }
             @Test
             void settle_FullPayment_SettlesAndSavesFee() {
                          when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
                          when(feeRepository.save(fee)).thenReturn(fee);
                          Fee result = feeService.settle(feeId,new BigDecimal("10000.00"));
                          assertEquals(new BigDecimal("10000.00"),result.getAmountPaid());
                          assertEquals(BigDecimal.ZERO.setScale(2),result.getOutstandingAmount());
                          assertEquals(FeeStatus.SETTLED, result.getStatus());
                          verify(feeRepository).findById(feeId);
                          verify(feeRepository).save(fee);
                        }

             @Test
             void settle_WhenAmountExceedsOutstanding_DoesNotSaveFee() {
                          when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
                          assertThrows(IllegalArgumentException.class, ()->
                        feeService.settle(feeId, new BigDecimal("12000.00"))
                    );
                          verify(feeRepository).findById(feeId);
                          verify(feeRepository,never()).save(any(Fee.class));
                        }

             @Test
             void findAllById_ReturnsFees() {
                          UUID secondStudentId=UUID.randomUUID();
                          Fee secondfee=new Fee(
                            secondStudentId,
                            institutionId,FeeType.ACTIVITIES,
                            "2026-2027",
                            "EGP",
                            new BigDecimal("2000.00"),
                            null
                        );
                        List<UUID> feeIds=List.of(UUID.randomUUID(), UUID.randomUUID());
                        List<Fee> expected=List.of(fee, secondfee);
                         when(feeRepository.findAllById(feeIds)).thenReturn(expected);
                         List<Fee> result =feeService.findAllById(feeIds);
                         assertEquals(expected, result);
                          verify(feeRepository).findAllById(feeIds);
                          
                        }
            
             @Test
             void findAllById_WhenIdsEmpty_ReturnsEmptyList() {
                      List<Fee> result =feeService.findAllById(List.of());
                      assertTrue(result.isEmpty());
                     verifyNoInteractions(feeRepository);
                    }









    
}
