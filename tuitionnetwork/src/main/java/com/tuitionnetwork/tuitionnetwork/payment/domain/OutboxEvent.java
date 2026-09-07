package com.tuitionnetwork.tuitionnetwork.payment.domain;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "outbox_event",schema = "payments")
public class OutboxEvent {
    @Id 
    @GeneratedValue 
    private UUID id;
    @Column (name ="aggregate_id", nullable = false)
    private UUID aggregateId;
    @Column(name="event_type", nullable = false)
    private String eventType;
    @Column ( nullable = false)
    private String payload;
    @Column ( nullable = false)
    private String status;
     @Column ( name= "created_at")
     private Instant createdAt;
     @Column (name= "published_at")
     private Instant publishedAt;

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAggregateId() {
        return this.aggregateId;
    }

    public void setAggregateId(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return this.eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return this.payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return this.publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
