package com.tuitionnetwork.tuitionnetwork.payment.receipt.domain;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
@Entity
@Table(name = "receipt", schema = "receipts")
public class Receipt {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    @Column(name = "reference_number", nullable = false, length = 50)
    private String referenceNumber;
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
    protected Receipt() {
    }
    public static Receipt issue(UUID paymentId, String referenceNumber) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId is required");
        }
        if (referenceNumber == null || referenceNumber.isBlank()) {
            throw new IllegalArgumentException("referenceNumber is required");
        }
        if (referenceNumber.length() > 50) {
            throw new IllegalArgumentException(
                    "referenceNumber exceeds the column length of 50");
        }

        Receipt receipt = new Receipt();
        receipt.paymentId = paymentId;
        receipt.referenceNumber = referenceNumber;
        return receipt;
    }
    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
