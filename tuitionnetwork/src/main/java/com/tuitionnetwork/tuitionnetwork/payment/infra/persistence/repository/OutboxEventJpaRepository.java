package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, UUID> {
  @Query(value = """
            SELECT * FROM payments.outbox_event
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimPendingBatch(@Param("limit") int limit);
 
    List<OutboxEvent> findByAggregateId(UUID aggregateId);
    
}
