package com.pck4x.transfers_service.infrastructure.persistence.mapper;

import com.pck4x.transfers_service.domain.TransferEvent;
import com.pck4x.transfers_service.infrastructure.persistence.jpa.entities.TransferEventEntity;

import java.time.LocalDateTime;

public class TransferEventMapper {

    public static final TransferEventMapper INSTANCE = new TransferEventMapper();

    private TransferEventMapper() {}

    public TransferEventEntity toEntity(TransferEvent event) {
        TransferEventEntity entity = new TransferEventEntity();
        entity.setId(event.getId());
        entity.setEventType(event.getEventType());
        entity.setPayload(event.getPayload());
        entity.setStatus(event.getStatus());
        entity.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt() : LocalDateTime.now());
        entity.setSentAt(event.getSentAt());
        return entity;
    }

    public TransferEvent toDomain(TransferEventEntity entity) {
        TransferEvent event = new TransferEvent();
        event.setId(entity.getId());
        event.setTransferId(entity.getTransfer().getTransferId());
        event.setEventType(entity.getEventType());
        event.setPayload(entity.getPayload());
        event.setStatus(entity.getStatus());
        event.setCreatedAt(entity.getCreatedAt());
        event.setSentAt(entity.getSentAt());
        return event;
    }
}
