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

    private static final int MAX_ATTEMPTS = 5;

    @Transactional
    @Scheduled(fixedDelay = 500)
    public void processPendingEvents() {
        var pendingEvents = outboxEventRepository.findRetryable(LocalDateTime.now());

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                var eventClass = AccountEventType.resolve(outboxEvent.getEventType());
                var event = objectMapper.readValue(outboxEvent.getPayload(), eventClass);

                kafkaTemplate.send(outboxEvent.getTopic(), event).get(10, TimeUnit.SECONDS);

                outboxEvent.setStatus(OutboxStatus.SENT);
                outboxEvent.setSentAt(LocalDateTime.now());
                outboxEvent.setLastError(null);
                outboxEventRepository.save(outboxEvent);

                log.info("Outbox event {} ({}) published to {}", outboxEvent.getId(),
                        outboxEvent.getEventType(), outboxEvent.getTopic());
            } catch (Exception e) {
                outboxEvent.setAttempts(outboxEvent.getAttempts() + 1);
                outboxEvent.setLastError(truncate(e.getMessage()));

                if (outboxEvent.getAttempts() >= MAX_ATTEMPTS) {
                    outboxEvent.setStatus(OutboxStatus.FAILED);
                    outboxEvent.setNextAttemptAt(LocalDateTime.now().plusDays(365));
                    log.error("DEAD LETTER outbox event {} ({}) after {} attempts: {}",
                            outboxEvent.getId(), outboxEvent.getEventType(),
                            outboxEvent.getAttempts(), outboxEvent.getLastError());
                } else {
                    outboxEvent.setStatus(OutboxStatus.PENDING);
                    outboxEvent.setNextAttemptAt(
                            LocalDateTime.now().plusSeconds(backoffSeconds(outboxEvent.getAttempts())));
                    log.warn("Outbox event {} will be retried in {} s (attempt {}/{}): {}",
                            outboxEvent.getId(), backoffSeconds(outboxEvent.getAttempts()),
                            outboxEvent.getAttempts(), MAX_ATTEMPTS, outboxEvent.getLastError());
                }
                outboxEventRepository.save(outboxEvent);
            }
        }
    }

    private long backoffSeconds(int attempts) {
        return Math.min(60, 1L << Math.min(attempts, 6));
    }

    private String truncate(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() > 900 ? message.substring(0, 900) : message;
    }
}
