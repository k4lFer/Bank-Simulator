package com.pck4x.transfers_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pck4x.sharedcontracts.event.TransferRequestedEvent;
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

    private static final String TRANSFER_TOPIC = "bank.transfer.events";

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

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void processPendingEvents() {
        var pendingEvents = transferEventRepository.findByStatus(OutboxStatus.PENDING);

        for (TransferEvent outboxEvent : pendingEvents) {
            try {
                var transferEvent = objectMapper.readValue(outboxEvent.getPayload(), TransferRequestedEvent.class);
                eventPublisher.publish(TRANSFER_TOPIC, transferEvent);

                outboxEvent.setStatus(OutboxStatus.SENT);
                outboxEvent.setSentAt(LocalDateTime.now());
                transferEventRepository.save(outboxEvent);

                log.info("Outbox event {} published successfully", outboxEvent.getId());
            } catch (Exception e) {
                log.error("Failed to process outbox event {}: {}", outboxEvent.getId(), e.getMessage(), e);
            }
        }
    }
}
