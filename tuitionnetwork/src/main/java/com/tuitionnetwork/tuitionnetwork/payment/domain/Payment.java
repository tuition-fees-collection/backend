package com.tuitionnetwork.tuitionnetwork.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "payment",schema = "payments")
public class Payment {
@Id
@GeneratedValue
private UUID id;
@Column(name = "idempotency_key", unique = true, nullable = false)
private String idempotencyKey;
@Column(name ="gateway_payment_id")
private String gatewayPaymentId;
@Column(name = "guardian_id", nullable = false)
private UUID guardianId;
@Column(name = "processed_by", nullable = false)
private UUID processedBy;
@Enumerated(EnumType.STRING)
@Column(name = "method", nullable = false)
private PaymentMethod method;
@Enumerated(EnumType.STRING)
@Column(name = "payment_type", nullable = false)
private PaymentType paymentType;
@Column(nullable=false)
private BigDecimal amount;
@Column(nullable=false)
private String currency;
@Enumerated(EnumType.STRING)
@Column(nullable=false)
private PaymentStatus status;
@Column(name ="auth_code")
private String authCode;
@Column(name ="rrn")
private String rrn;
@Column(name ="response_code")
private String responseCode;
@Column(name ="response_message")
private String responseMessage;
@Column(name ="masked_card_number")
private String maskedCardNumber;
@Column(name ="card_scheme")
private String cardScheme;
@Column(name ="captured_amount")
private BigDecimal capturedAmount;
@Column(name ="refunded_amount")
private BigDecimal refundedAmount;
@Column(name ="created_at")
private Instant createdAt;
@Column(name = "updated_at")
private Instant updatedAt;
@Column(name = "settled_at")
private Instant settledAt;
@Version
private Integer version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
       

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return this.idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getGatewayPaymentId() {
        return this.gatewayPaymentId;
    }

    public void setGatewayPaymentId(String gatewayPaymentId) {
        this.gatewayPaymentId = gatewayPaymentId;
    }

    public UUID getGuardianId() {
        return this.guardianId;
    }

    public void setGuardianId(UUID guardianId) {
        this.guardianId = guardianId;
    }

    public UUID getProcessedBy() {
        return this.processedBy;
    }

    public void setProcessedBy(UUID processedBy) {
        this.processedBy = processedBy;
    }

    public PaymentMethod getMethod() {
        return this.method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentType getPaymentType() {
        return this.paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return this.status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getAuthCode() {
        return this.authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getRrn() {
        return this.rrn;
    }

    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    public String getResponseCode() {
        return this.responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return this.responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getMaskedCardNumber() {
        return this.maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    public String getCardScheme() {
        return this.cardScheme;
    }

    public void setCardScheme(String cardScheme) {
        this.cardScheme = cardScheme;
    }

    public BigDecimal getCapturedAmount() {
        return this.capturedAmount;
    }

    public void setCapturedAmount(BigDecimal capturedAmount) {
        this.capturedAmount = capturedAmount;
    }

    public BigDecimal getRefundedAmount() {
        return this.refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getSettledAt() {
        return this.settledAt;
    }

    public void setSettledAt(Instant settledAt) {
        this.settledAt = settledAt;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

}
