package com.pck4x.accounts_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pck4x.accounts_service.application.port.output.EventPublisher;
import com.pck4x.accounts_service.application.port.output.OutboxEventRepository;
import com.pck4x.accounts_service.domain.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AccountEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public AccountEventPublisher(OutboxEventRepository outboxEventRepository,
                                  ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, Object event) {
        try {
            var payload = objectMapper.writeValueAsString(event);
            var eventType = event.getClass().getName();
            var outboxEvent = new OutboxEvent(topic, eventType, payload);
            outboxEventRepository.save(outboxEvent);
            log.debug("Event saved to outbox: topic={}, eventType={}", topic, eventType);
        } catch (Exception e) {
            log.error("Failed to save event to outbox: topic={}, event={}", topic, event, e);
            throw new RuntimeException("Failed to persist outbox event", e);
        }
    }
}
