package com.tuitionnetwork.tuitionnetwork.payment.domain;
import java.math.BigDecimal;
import java.time.Duration;
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
    //constansts so payment expires after 15 minutes if not completed, to avoid stale payments
 public static final Duration ORDER_VALIDITY = Duration.ofMinutes(15);
 //set a max length for response message to avoid database errors
 private static final int RESPONSE_MESSAGE_MAX = 255;
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
@Column(nullable=false, precision=12, scale=2)
private BigDecimal amount;
@Column(nullable=false, length=3)
private String currency = "EGP";
@Enumerated(EnumType.STRING)
@Column(nullable=false, length = 20)
private PaymentStatus status = PaymentStatus.CREATED;
@Column(name ="auth_code", length = 50)
private String authCode;
@Column(name ="rrn", length = 50)
private String rrn;
@Column(name ="response_code", length = 10)
private String responseCode;
@Column(name ="response_message", length = RESPONSE_MESSAGE_MAX)
private String responseMessage;
@Column(name ="masked_card_number", length = 30)
private String maskedCardNumber;
@Column(name ="card_scheme", length = 20)
private String cardScheme;
@Column(name ="captured_amount", precision = 12, scale = 2)
private BigDecimal capturedAmount;
@Column(name ="created_at", nullable = false)
private Instant createdAt;
@Column(name = "updated_at")
private Instant updatedAt;
@Column(name = "settled_at")
private Instant settledAt;
@Version
private Integer version;
protected Payment() {
    } 
// method enforcing validation before constructing object
public static Payment create(String idempotencyKey,
                                 UUID guardianId,
                                 UUID processedBy,
                                 PaymentMethod method,
                                 PaymentType paymentType,
                                 BigDecimal amount,
                                 String currency) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (guardianId == null) {
            throw new IllegalArgumentException("guardianId is required");
        }
        if (processedBy == null) {
            throw new IllegalArgumentException(
                    "processedBy is NOT NULL in the schema; must be provided");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Payment amount must not exceed 2 decimal places");
        }
        //make object 
        Payment payment = new Payment();
        payment.idempotencyKey = idempotencyKey;
        payment.guardianId = guardianId;
        payment.processedBy = processedBy;
        payment.method = method;
        payment.paymentType = paymentType;
        payment.amount = amount;
        payment.currency = (currency == null || currency.trim().isEmpty()) ? "EGP" : currency;
        payment.status = PaymentStatus.CREATED;
        return payment;
    }
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = PaymentStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
       
  public void markSuccessful(String gatewayPaymentId,
                               BigDecimal capturedAmount,
                               String authCode,
                               String rrn,
                               String responseCode,
                               String responseMessage,
                               String maskedCardNumber,
                               String cardScheme) {
        applyGatewayResponse(gatewayPaymentId, capturedAmount, authCode, rrn,
                responseCode, responseMessage, maskedCardNumber, cardScheme);
        transitionTo(PaymentStatus.SUCCESSFUL);
        this.settledAt = Instant.now();
    }
 public void markSuccessfulCash() {
        this.capturedAmount = this.amount;
        transitionTo(PaymentStatus.SUCCESSFUL);
        this.settledAt = Instant.now();
    }
    public void markFailed(String reason, String responseCode) {
        setResponseMessage(reason);
        if (responseCode != null) {
            this.responseCode = responseCode;
        }
        transitionTo(PaymentStatus.FAILED);
    }
     public void flagForReconciliation(String reason) {
        setResponseMessage(reason);
        transitionTo(PaymentStatus.RECON_REQUIRED);
    }
    public void resolveReconciliation(PaymentStatus resolution, String gatewayPaymentId) {
        if (status != PaymentStatus.RECON_REQUIRED) {
            throw new IllegalStateException("Only a flagged payment can be reconciled");
        }
        if (resolution == PaymentStatus.SUCCESSFUL) {
            if (gatewayPaymentId != null) {
                this.gatewayPaymentId = gatewayPaymentId;
            }
            transitionTo(PaymentStatus.SUCCESSFUL);
            this.settledAt = Instant.now();
        } else if (resolution == PaymentStatus.FAILED) {
            transitionTo(PaymentStatus.FAILED);
        } else {
            throw new IllegalArgumentException("Reconciliation resolves to SUCCESSFUL or FAILED only");
        }
    }
    private void transitionTo(PaymentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalPaymentTransitionException(id, status, target);
        }
        this.status = target;
    }
 
    private void applyGatewayResponse(String gatewayPaymentId, BigDecimal capturedAmount,
                                      String authCode, String rrn, String responseCode,
                                      String responseMessage, String maskedCardNumber,
                                      String cardScheme) {
        if (gatewayPaymentId != null) this.gatewayPaymentId = gatewayPaymentId;
        if (capturedAmount != null) this.capturedAmount = capturedAmount;
        if (authCode != null) this.authCode = authCode;
        if (rrn != null) this.rrn = rrn;
        if (responseCode != null) this.responseCode = responseCode;
        if (maskedCardNumber != null) this.maskedCardNumber = maskedCardNumber;
        if (cardScheme != null) this.cardScheme = cardScheme;
        setResponseMessage(responseMessage);
    }
     private void setResponseMessage(String message) {
        if (message == null) {
            return;
        }
        this.responseMessage = message.length() > RESPONSE_MESSAGE_MAX
                ? message.substring(0, RESPONSE_MESSAGE_MAX)
                : message;
    }
 
    // Set once, when the gateway first answers with an id, before the outcome is known. 
    public void attachGatewayPaymentId(String gatewayPaymentId) {
        this.gatewayPaymentId = gatewayPaymentId;
    } 
    //add 15 mins
  public Instant getExpiresAt() {
        return createdAt == null ? null : createdAt.plus(ORDER_VALIDITY);
    }
 //check lw 3adah 15 mins
    public boolean isExpired(Instant now) {
        Instant expiry = getExpiresAt();
        return status == PaymentStatus.CREATED && expiry != null && now.isAfter(expiry);
    }
 
    //A receipt exists only for a confirmed payment
    public boolean isReceiptEligible() {
        return status.isSettled();
    }
 
    //** EPP needs a credit card and a confirmed charge 
    public boolean isEppEligible() {
        return status.isSettled() && method != null && method.isEppEligible();
    }
 public void markPending() {
    if (isExpired(Instant.now())) {
        throw new IllegalStateException("Payment order has expired");
    }
    transitionTo(PaymentStatus.PENDING);
}
    public UUID getId() { 
        return id; 
    }
    public String getIdempotencyKey() { 
        return idempotencyKey;
     }
    public String getGatewayPaymentId() { 
        return gatewayPaymentId;
     }
    public UUID getGuardianId() { 
        return guardianId; 
    }
    public UUID getProcessedBy() {
         return processedBy; 
        }
    public PaymentMethod getMethod() { 
        return method; 
    }
    public PaymentType getPaymentType() {
         return paymentType;
         }
    public BigDecimal getAmount() {
         return amount; 
        }
    public String getCurrency() { 
        return currency; 
    }
    public PaymentStatus getStatus() { 
        return status;
     }
    public String getAuthCode() { 
        return authCode; 
    }
    public String getRrn() { 
        return rrn; 
    }
    public String getResponseCode() { 
        return responseCode;
     }
    public String getResponseMessage() {
         return responseMessage; 
        }
    public String getMaskedCardNumber() { 
        return maskedCardNumber;
     }
    public String getCardScheme() { 
        return cardScheme; 
    }
    public BigDecimal getCapturedAmount() {
         return capturedAmount; 
        }
    public Instant getCreatedAt() { 
        return createdAt; 
    }
    public Instant getUpdatedAt() { 
        return updatedAt; 
    }
    public Instant getSettledAt() {
         return settledAt; 
        }
    public Integer getVersion() {
         return version; 
        }
}
