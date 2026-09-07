package com.tuitionnetwork.tuitionnetwork.payment.domain;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity 
@Table (name ="payment_allocation", schema ="payments")
public class PaymentAllocation {
    @Id
    @GeneratedValue
    private UUID id;
    @Column (name ="payment_id", nullable = false)
    private UUID paymentId;
    @Column (name ="fee_id", nullable = false)
    private UUID feeId;
    @Column (name="amount_applied", nullable = false)
    private BigDecimal amountApplied;

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPaymentId() {
        return this.paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public UUID getFeeId() {
        return this.feeId;
    }

    public void setFeeId(UUID feeId) {
        this.feeId = feeId;
    }

    public BigDecimal getAmountApplied() {
        return this.amountApplied;
    }

    public void setAmountApplied(BigDecimal amountApplied) {
        this.amountApplied = amountApplied;
    }

 
}
