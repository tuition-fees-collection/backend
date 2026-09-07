package com.tuitionnetwork.tuitionnetwork.payment.domain;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "payment_saga_steps",schema = "payments")
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
@Column(name="attempt_at")
private String attemptAt; 
@Column(name="completed_at")
private String completedAt;
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

    public String getAttemptAt() {
        return this.attemptAt;
    }

    public void setAttemptAt(String attemptAt) {
        this.attemptAt = attemptAt;
    }

    public String getCompletedAt() {
        return this.completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
 
}
