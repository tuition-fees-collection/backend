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
@Column(name="step_name", nullable = false)
    private String stepName;
@Column(nullable = false)
    private String status;
@Column(name="attempted_at")
private Instant attemptedAt; 
@Column(name="completed_at")
private Instant completedAt;
@Column (name="error_message")
private String errorMessage;
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

    public String getStepName() {
        return this.stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getAttemptedAt() {
        return this.attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public Instant getCompletedAt() {
        return this.completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
