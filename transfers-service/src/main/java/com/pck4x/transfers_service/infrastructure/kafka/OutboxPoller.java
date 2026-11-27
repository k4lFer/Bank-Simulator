package com.pck4x.transfers_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pck4x.transfers_service.application.port.output.EventPublisher;
import com.pck4x.transfers_service.application.port.output.TransferEventRepository;
import com.pck4x.transfers_service.domain.TransferEvent;
import com.pck4x.transfers_service.domain.enums.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final TransferEventRepository transferEventRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxPoller(TransferEventRepository transferEventRepository,
                        EventPublisher eventPublisher,
                        ObjectMapper objectMapper) {
        this.transferEventRepository = transferEventRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    private static final int MAX_ATTEMPTS = 5;

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        var pendingEvents = transferEventRepository.findRetryable(LocalDateTime.now());

        for (TransferEvent outboxEvent : pendingEvents) {
            try {
                // Fase 2.3: el tipo del outbox decide si es comando o evento y su topic.
                var outboxType = TransferOutboxType.resolve(outboxEvent.getEventType());
                var message = objectMapper.readValue(outboxEvent.getPayload(), outboxType.messageClass());
                eventPublisher.publish(outboxType.topic(), message);

                outboxEvent.setStatus(OutboxStatus.SENT);
                outboxEvent.setSentAt(LocalDateTime.now());
                outboxEvent.setLastError(null);
                transferEventRepository.save(outboxEvent);

                log.info("Outbox event {} published successfully", outboxEvent.getId());
            } catch (Exception e) {
                outboxEvent.setAttempts(outboxEvent.getAttempts() + 1);
                outboxEvent.setLastError(truncate(e.getMessage()));

                if (outboxEvent.getAttempts() >= MAX_ATTEMPTS) {
                    outboxEvent.setStatus(OutboxStatus.FAILED);
                    outboxEvent.setNextAttemptAt(LocalDateTime.now().plusDays(365));
                    log.error("DEAD LETTER outbox event {} after {} attempts: {}",
                            outboxEvent.getId(), outboxEvent.getAttempts(), outboxEvent.getLastError());
                } else {
                    outboxEvent.setStatus(OutboxStatus.PENDING);
                    outboxEvent.setNextAttemptAt(
                            LocalDateTime.now().plusSeconds(backoffSeconds(outboxEvent.getAttempts())));
                    log.warn("Outbox event {} will be retried in {} s (attempt {}/{}): {}",
                            outboxEvent.getId(), backoffSeconds(outboxEvent.getAttempts()),
                            outboxEvent.getAttempts(), MAX_ATTEMPTS, outboxEvent.getLastError());
                }
                transferEventRepository.save(outboxEvent);
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
