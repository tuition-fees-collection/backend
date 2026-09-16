package com.tuitionnetwork.tuitionnetwork.billing.service;


import org.springframework.stereotype.Service;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.infra.FeeRepository;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeType;

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

    /*get a fee by id */
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
    
    /*get payable fees only (Read/Check*/
    public Fee getPayableFee(UUID feeId){
        Fee fee=getFee(feeId);
        ensureFee(fee);
        return fee;

    }

    /*get payable fees and lock it at payment processing */
    public Fee getPayableFeeForUpdate(UUID feeId){
        Fee fee=feeRepository.findByIdForUpdate(feeId)
        .orElseThrow(() -> new IllegalArgumentException("Fee not found: "+feeId));
        ensureFee(fee);

        return fee;
    }

    /*get all fees for a specific student */
    public List<Fee> getFeesForStudent(UUID studentId){
        return feeRepository.findByStudentId(studentId);
    }

    /*get payable fees for a specific student */
    public List<Fee> getPayableFeesForStudent(UUID studentId){
        return feeRepository.findPayableFeeByStudentId(studentId);
    }

    /*get all payable fees for multiple students(for gurdian's fee view) */
    public List<Fee> getPayableFeesForStudents(List<UUID> studentIds){
        return feeRepository.findPayableFeesByStudentIds(studentIds);
    }

    /*get outstanding amount fee for a fee */
    public BigDecimal getOutstandingAmount(UUID feeId){
        Fee fee=getFee(feeId);
        return fee.getOutstandingAmount();
    }

    /* get a student's fee record for a specific institution */
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

    /*validate payment amount before processing */
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

    /* get institution collections with optional fee type and period filters */
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