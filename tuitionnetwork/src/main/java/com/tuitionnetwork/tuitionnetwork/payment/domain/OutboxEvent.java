package com.tuitionnetwork.tuitionnetwork.payment.domain;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
@Entity
@Table(name = "outbox_event",schema = "payments")
public class OutboxEvent {
      public enum Status {
        PENDING,
        PUBLISHED,
        FAILED
    }
    @Id 
    @GeneratedValue 
    private UUID id;
    @Column (name ="aggregate_id", nullable = false)
    private UUID aggregateId;
    @Column(name="event_type", nullable = false, length=50)
    private String eventType;
    @Column ( nullable = false, columnDefinition = "TEXT")
    private String payload;
   @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;
     @Column ( name= "created_at",nullable = false)
     private Instant createdAt;
     @Column (name= "published_at")
     private Instant publishedAt;

    protected OutboxEvent() {
    }
    public static OutboxEvent of(UUID paymentId, String eventType, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateId = paymentId;
        event.eventType = eventType;
        event.payload = payload;
        event.status = Status.PENDING;
        return event;
    }
 
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = Status.PENDING;
        }
    }
    public void markPublished(Instant now) {
        this.status = Status.PUBLISHED;
        this.publishedAt = now;
    }
    public void markFailed() {
        this.status = Status.FAILED;
    }
 
    public UUID getId() { 
        return id;
     }
    public UUID getAggregateId() { 
        return aggregateId;
     }
    public String getEventType() { 
        return eventType; 
    }
    public String getPayload() { 
        return payload; 
    }
    public Status getStatus() {
         return status; 
        }
    public Instant getCreatedAt() { 
        return createdAt; 
    }
    public Instant getPublishedAt() {
         return publishedAt; 
        }
}
