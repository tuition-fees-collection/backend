package com.tuitionnetwork.tuitionnetwork.billing.service;


import org.springframework.stereotype.Service;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.infra.FeeRepository;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;



@Service
@Transactional 

public class FeeService{
    private final FeeRepository feeRepository;

    public FeeService(FeeRepository feeRepository){
        this.feeRepository=feeRepository;
    }

    
    public Fee getFee(UUID feeId){
        return feeRepository.findById(feeId)
        .orElseThrow(() -> new IllegalArgumentException("Fee not found: "+feeId));
    }

    private void ensureFee(Fee fee){
        if(fee.getStatus()==FeeStatus.SETTLED){
            throw new IllegalArgumentException("Fee is already paid: "+fee.getId());
        }
        if(fee.getOutstandingAmount().signum()<=0){
            throw new IllegalArgumentException("Fee has no outstanding balance "+fee.getId());
        }

    }
    
    
    public Fee getPayableFee(UUID feeId){
        Fee fee=getFee(feeId);
        ensureFee(fee);
        return fee;

    }


    
    public List<Fee> getFeesForStudent(UUID studentId){
        return feeRepository.findByStudentId(studentId);
    }

    
    public List<Fee> getPayableFeesForStudent(UUID studentId){
        return feeRepository.findPayableFeeByStudentId(studentId);
    }

    
    public List<Fee> getPayableFeesForStudents(List<UUID> studentIds){
        return feeRepository.findPayableFeesByStudentIds(studentIds);
    }

   
    public BigDecimal getOutstandingAmount(UUID feeId){
        Fee fee=getFee(feeId);
        return fee.getOutstandingAmount();
    }

    
    public List<Fee> getFeesForStudentAndInstitution(UUID studentId,UUID institutionId) {
        List<Fee> fees=
                feeRepository.findByStudentIdAndInstitutionId(
                        studentId,
                        institutionId
                );

                if(fees.isEmpty()) {
                      throw new FeeNotFoundException(
                         "Student fee record not found"
                        );
                }
                return fees;
   
}
          public Fee settle(UUID feeId,BigDecimal amount,int expectedVersion){
            Fee fee=getPayableFee(feeId);
            int currentVersion=fee.getVersion()==null?0:fee.getVersion();
            if(currentVersion!=expectedVersion){
                throw new Concurrentsettlementexception(
                    "Fee "+feeId+" was modified by another payment (expected version "
                        +expectedVersion+" but found "+currentVersion+")");
            }
            fee.applyPayment(amount);
            try{
                return feeRepository.saveAndFlush(fee);
            }catch(OptimisticLockingFailureException e){
                throw new Concurrentsettlementexception(
                    "Fee "+feeId+" was modified by another payment", e);
            }
          }

          
         
          @Transactional(readOnly = true)
          public List<Fee> findAllById(List<UUID> feeIds){
            if(feeIds==null||feeIds.isEmpty()){
                return List.of();
            }
            return feeRepository.findAllById(feeIds);
          }

    

   
    public void validatePaymentAmount(UUID feeId,BigDecimal paymentAmount){
        Fee fee=getPayableFee(feeId);
        if(paymentAmount==null||paymentAmount.signum()<=0){
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
           /*payment amount>outstanding amount */
         if (paymentAmount.compareTo(fee.getOutstandingAmount())>0) {
            throw new IllegalArgumentException("Payment amount exceeds outstanding amount");
        }
    }

   
    public List<Fee> getInstitutionCollections(
         UUID institutionId,
        FeeStatus feeStatus,
        FeeType feeType,
        String collectionPeriod 
    ){
        if(institutionId==null){
            throw new IllegalArgumentException("Institution ID is required");
        }
        if(feeStatus==null){
            throw new IllegalArgumentException("Fee status is required");
        }
        return feeRepository.findInstitutionCollections(
            institutionId,
             feeStatus, 
             feeType, 
             collectionPeriod);
       
    }

}