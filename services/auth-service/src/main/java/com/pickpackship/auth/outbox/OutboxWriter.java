package com.pickpackship.auth.outbox;

import com.pickpackship.auth.domain.OutboxEvent;
import com.pickpackship.auth.repository.OutboxEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void write(String eventType, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setPayload(objectMapper.writeValueAsString(payload));
        event.setPublished(false);
        event.setCreatedAt(Instant.now());
        outboxEventRepository.save(event);
    }
}
