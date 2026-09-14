package com.tuitionnetwork.tuitionnetwork.payment.port.portout;
import java.util.List;
import java.util.UUID;
import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
public interface OutboxEventPort {
    //save events
    OutboxEvent save(OutboxEvent event);
    //retiervie pemnding event as batch and tell limit not alll in memory
    List<OutboxEvent> findPendingBatch(int limit);
    List<OutboxEvent> findByAggregateId(UUID paymentId);
}
