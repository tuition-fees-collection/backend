package com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.adapter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
import com.tuitionnetwork.tuitionnetwork.payment.infra.persistence.repository.OutboxEventJpaRepository;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.OutboxEventPort;
@Component 
public class OutboxEventPersistenceAdapter implements OutboxEventPort{
    private final OutboxEventJpaRepository jpa;
    public OutboxEventPersistenceAdapter(OutboxEventJpaRepository jpa) {
        this.jpa = jpa;
    }
 
    @Override
    public OutboxEvent save(OutboxEvent event) {
        return jpa.save(event);
    }
    @Override
    public List<OutboxEvent> findPendingBatch(int limit) {
        return jpa.claimPendingBatch(limit);
    }
 
    @Override
    public List<OutboxEvent> findByAggregateId(UUID paymentId) {
        return jpa.findByAggregateId(paymentId);
    }
}
