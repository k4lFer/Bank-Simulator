package com.pck4x.accounts_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pck4x.accounts_service.application.port.output.OutboxEventRepository;
import com.pck4x.accounts_service.domain.OutboxEvent;
import com.pck4x.accounts_service.domain.enums.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPoller(OutboxEventRepository outboxEventRepository,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Scheduled(fixedDelay = 500)
    public void processPendingEvents() {
        var pendingEvents = outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                var eventClass = Class.forName(outboxEvent.getEventType());
                var event = objectMapper.readValue(outboxEvent.getPayload(), eventClass);

                kafkaTemplate.send(outboxEvent.getTopic(), event).get(10, TimeUnit.SECONDS);

                outboxEvent.setStatus(OutboxStatus.SENT);
                outboxEvent.setSentAt(LocalDateTime.now());
                outboxEventRepository.save(outboxEvent);

                log.info("Outbox event {} ({}) published to {}", outboxEvent.getId(),
                        outboxEvent.getEventType(), outboxEvent.getTopic());
            } catch (ClassNotFoundException e) {
                log.error("Event class not found for outbox event {}: {}",
                        outboxEvent.getId(), outboxEvent.getEventType(), e);
                outboxEvent.setStatus(OutboxStatus.FAILED);
                outboxEventRepository.save(outboxEvent);
            } catch (Exception e) {
                log.error("Failed to process outbox event {}: {}",
                        outboxEvent.getId(), e.getMessage(), e);
            }
        }
    }
}
