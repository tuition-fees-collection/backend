package com.tuitionnetwork.tuitionnetwork.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;



@Entity
@Table(name="fee",schema = "billing")

public class Fee{
       @Id
       @GeneratedValue(strategy=GenerationType.UUID)
       private UUID id;

       @Column(name="student_id",nullable=false)
       private UUID studentId;

       @Column(name="institution_id",nullable=false)
       private UUID institutionId;

       @Enumerated(EnumType.STRING)
       @Column(name="fee_type",nullable=false)
       private FeeType feeType;

       @Column(name="collection_period",nullable=false)
       private String collectionPeriod;

       @Column(name="currency",nullable=false)
       private String currency;

       @Column(name="amount",nullable=false,precision = 19,scale = 2)
       private BigDecimal amount;

       @Column(name="amount_paid",nullable=false,precision=19,scale=2)
       private BigDecimal amountPaid;

       @Enumerated(EnumType.STRING)
       @Column(name="status",nullable=false)
       private FeeStatus status;

       @Column(name="source_batch_id")
       private UUID sourceBatchId;

       @Version
       @Column(name="version",nullable=false)
       private Integer version;

       protected Fee(){}

       public Fee(
        UUID studentId,
        UUID institutionId,
        FeeType feeType,
        String collectionPeriod,
        String currency,
        BigDecimal amount,
        UUID sourceBatchId

        ){
            this.studentId=studentId;
            this.institutionId=institutionId;
            this.feeType=feeType;
            this.collectionPeriod=collectionPeriod;
            this.currency=currency;
            this.amount=amount;
            this.amountPaid=BigDecimal.ZERO;
            this.status=FeeStatus.OUTSTANDING;
            this.sourceBatchId=sourceBatchId;
            this.version=0;

        }
    public UUID getId(){
        return id;
    }

    public UUID getStudentId(){
        return studentId;
    }

    public UUID getInstitutionId() {
        return institutionId;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public String getCollectionPeriod() {
        return collectionPeriod;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public FeeStatus getStatus() {
        return status;
    }

    public UUID getSourceBatchId() {
        return sourceBatchId;
    }

    public Integer getVersion() {
        return version;
    }

    public BigDecimal getOutstandingAmount() {
        return amount.subtract(amountPaid);
    }

    public void applyPayment(BigDecimal paymentAmount){
        if(paymentAmount==null||paymentAmount.signum()<=0){
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
       
         if (paymentAmount.compareTo(getOutstandingAmount()) > 0) {
        throw new IllegalArgumentException(
            "Payment amount cannot exceed outstanding balance");
        }
        
        this.amountPaid=this.amountPaid.add(paymentAmount);
        if(this.amountPaid.compareTo(this.amount)==0){
            this.status=FeeStatus.SETTLED;
        }else{
            this.status=FeeStatus.PARTIALLY_PAID;
        }
    }
}




