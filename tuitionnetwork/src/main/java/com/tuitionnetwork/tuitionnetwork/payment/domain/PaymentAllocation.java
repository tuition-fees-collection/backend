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
    @Column (name="amount_applied", nullable = false, precision = 12, scale =2)
    private BigDecimal amountApplied;

    protected  PaymentAllocation() {
    }
public static PaymentAllocation of(UUID paymentId, UUID feeId, BigDecimal amountApplied) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId is required");
        }
        if (feeId == null) {
            throw new IllegalArgumentException("feeId is required");
        }
        if (amountApplied == null || amountApplied.signum() <= 0) {
            throw new IllegalArgumentException("amountApplied must be greater than zero");
        }
        if (amountApplied.scale() > 2) {
            throw new IllegalArgumentException(
                    "amountApplied must not exceed 2 decimal places - the column is NUMERIC(12,2)");
        }
 
        PaymentAllocation allocation = new PaymentAllocation();
        allocation.paymentId = paymentId;
        allocation.feeId = feeId;
        allocation.amountApplied = amountApplied;
        return allocation;
    }
    public UUID getId() {
        return this.id;
    }
    public UUID getPaymentId() {
        return this.paymentId;
    }
    public UUID getFeeId() {
        return this.feeId;
    }
    public BigDecimal getAmountApplied() {
        return this.amountApplied;
    }
}
