package com.tuitionnetwork.tuitionnetwork.payment.domain;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "payment_saga_step",schema = "payments")
public class PaymentSagaStep {
@Id 
@GeneratedValue 
private UUID id;
@Column (name = "payment_id", nullable = false)
    private UUID paymentId;
@Column(name="step_name", nullable = false, length =50)
    private PaymentSagaStepType stepName;
@Column(nullable = false, length = 20)
   private PaymentSagaStepStatus status = PaymentSagaStepStatus.PENDING;
@Column(name="attempted_at")
private Instant attemptedAt; 
@Column(name="completed_at")
private Instant completedAt;
@Column (name="error_message", columnDefinition = "TEXT")
private String errorMessage;

    protected PaymentSagaStep() {
    }

    public static PaymentSagaStep plan(UUID paymentId, PaymentSagaStepType stepName) {
        PaymentSagaStep step = new PaymentSagaStep();
        step.paymentId = paymentId;
        step.stepName = stepName;
        step.status = PaymentSagaStepStatus.PENDING;
        return step;
    }
      public void start(Instant now) {
        requireNotFinished();
        this.status = PaymentSagaStepStatus.IN_PROGRESS;
        this.attemptedAt = now;
    }
 
    public void complete(Instant now) {
        requireNotFinished();
        this.status = PaymentSagaStepStatus.COMPLETED;
        this.completedAt = now;
        this.errorMessage = null;
    }
 
    public void fail(String reason, Instant now) {
        requireNotFinished();
        this.status = PaymentSagaStepStatus.FAILED;
        this.completedAt = now;
        this.errorMessage = reason;
    }
 
    public void skip(String reason, Instant now) {
        requireNotFinished();
        this.status = PaymentSagaStepStatus.SKIPPED;
        this.completedAt = now;
        this.errorMessage = reason;
    }
 
    private void requireNotFinished() {
        if (status != null && status.isFinished()) {
            throw new IllegalStateException(
                    "Saga step " + stepName + " on payment " + paymentId + " is already " + status);
        }
    }
 
    
    public boolean isUnresolvedCharge() {
        return stepName != null && stepName.isMoneyMoving()
                && status == PaymentSagaStepStatus.IN_PROGRESS;
    }
 
    public UUID getId() { 
        return id; 
    }
    public UUID getPaymentId() { 
        return paymentId;
     }
    public PaymentSagaStepType getStepName() {
         return stepName; 
        }
    public PaymentSagaStepStatus getStatus() {
         return status; 
        }
    public Instant getAttemptedAt() { 
        return attemptedAt; 
    }
    public Instant getCompletedAt() { 
        return completedAt; 
    }
    public String getErrorMessage() {
         return errorMessage; 
        }
}
