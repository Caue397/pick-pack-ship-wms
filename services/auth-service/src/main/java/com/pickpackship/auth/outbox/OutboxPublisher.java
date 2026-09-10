package com.pickpackship.auth.outbox;

import com.pickpackship.auth.domain.OutboxEvent;
import com.pickpackship.auth.repository.OutboxEventRepository;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int BATCH_SIZE = 50;
    private static final long POLL_INTERVAL_MS = 24 * 60 * 60 * 1000L;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    @Scheduled(fixedDelay = POLL_INTERVAL_MS)
    public void publishPending() {
        List<OutboxEvent> batch = outboxEventRepository.lockNextBatch(BATCH_SIZE);

        for (OutboxEvent event : batch) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getPayload()).get();
                event.setPublished(true);
            } catch (ExecutionException e) {
                log.error("Failed to publish outbox event {} ({}), will retry next poll",
                        event.getId(), event.getEventType(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while publishing outbox event {}", event.getId(), e);
            }
        }
    }
}
