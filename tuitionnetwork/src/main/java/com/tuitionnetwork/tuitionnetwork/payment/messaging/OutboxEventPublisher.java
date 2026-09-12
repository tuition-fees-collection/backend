package com.tuitionnetwork.tuitionnetwork.payment.messaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.payment.domain.OutboxEvent;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.OutboxEventPort;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component 
public class OutboxEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
 
    private final OutboxEventPort outbox;
    private final int batchSize;
 
    public OutboxEventPublisher(OutboxEventPort outbox,
                                @Value("${payments.outbox.batch-size:50}") int batchSize) {
        this.outbox = outbox;
        this.batchSize = batchSize;
    }
    @Scheduled(fixedDelayString = "${payments.outbox.poll-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outbox.findPendingBatch(batchSize);
        if (batch.isEmpty()) {
            return;
        }
 
        log.debug("Publishing {} outbox event(s)", batch.size());
 
        for (OutboxEvent event : batch) {
            try {
                deliver(event);
                event.markPublished(Instant.now());
 
            } 
            catch (RuntimeException e) {
                log.error("Failed to publish outbox event {} of type {} for payment {}; "
                                + "leaving PENDING for retry",
                        event.getId(), event.getEventType(), event.getAggregateId(), e);
            }
        }
    }
      private void deliver(OutboxEvent event) {
        log.info("Outbox event {} [{}] for payment {}: {}",
                event.getId(), event.getEventType(), event.getAggregateId(), event.getPayload());
    }
}
