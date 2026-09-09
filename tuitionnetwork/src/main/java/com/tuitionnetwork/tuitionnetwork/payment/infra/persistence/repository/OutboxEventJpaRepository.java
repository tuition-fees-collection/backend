package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
import java.util.List;
import java.util.UUID;
public interface  OutboxEventJpaRepository  extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status); //fetch all unprocessed outbox events
    List<OutboxEvent> findByAggregateId(UUID aggregateId); //fetch all outbox events for a given aggregate id

    
}
