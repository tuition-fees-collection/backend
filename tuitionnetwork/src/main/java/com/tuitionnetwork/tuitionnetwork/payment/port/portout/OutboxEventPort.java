package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.util.List;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
public interface OutboxEventPort {
    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> findPendingBatch(int limit);
    List<OutboxEvent> findByAggregateId(UUID paymentId);
}
