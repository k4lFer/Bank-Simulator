package com.pck4x.transfers_service.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pck4x.sharedcontracts.command.SagaCommand;
import com.pck4x.sharedcontracts.event.DomainEvent;
import com.pck4x.sharedcontracts.security.CorrelationContext;
import com.pck4x.transfers_service.application.port.output.TransferEventRepository;
import com.pck4x.transfers_service.domain.TransferEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fase 2.3: unico punto de escritura de mensajes salientes en el outbox de transfers.
 * Separar comandos de eventos permite al poller elegir el topic correcto.
 */
@Component
public class TransferCommandPublisher {

    private final TransferEventRepository transferEventRepository;
    private final ObjectMapper objectMapper;

    public TransferCommandPublisher(TransferEventRepository transferEventRepository, ObjectMapper objectMapper) {
        this.transferEventRepository = transferEventRepository;
        this.objectMapper = objectMapper;
    }

    public TransferEvent enqueue(UUID transferId, Object message) {
        CorrelationContext.stamp(message);
        try {
            var payload = objectMapper.writeValueAsString(message);
            UUID messageId = message instanceof SagaCommand command ? command.getCommandId()
                    : message instanceof DomainEvent event ? event.getEventId() : null;
            var outboxEvent = new TransferEvent(
                    transferId, TransferOutboxType.nameOf(message), payload, messageId);
            return transferEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transfer message", e);
        }
    }
}
