package com.tuitionnetwork.tuitionnetwork.billing.service;


import org.springframework.stereotype.Service;
import com.tuitionnetwork.tuitionnetwork.billing.domain.Fee;
import com.tuitionnetwork.tuitionnetwork.billing.infra.FeeRepository;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.billing.domain.FeeStatus;
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
        if(fee.getStatus()==FeeStatus.PAID){
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


    public Fee getPayableFeeForUpdate(UUID feeId){
        Fee fee=feeRepository.findByIdForUpdate(feeId)
        .orElseThrow(() -> new IllegalArgumentException("Fee not found: "+feeId));
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


    public void validatePaymentAmount(UUID feeId,BigDecimal paymentAmount){
        Fee fee=getPayableFee(feeId);
        if(paymentAmount==null||paymentAmount.signum()<=0){
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

         if (paymentAmount.compareTo(fee.getOutstandingAmount())>0) {
            throw new IllegalArgumentException("Payment amount exceeds outstanding amount");
        }
    }

}