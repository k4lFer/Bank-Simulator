package com.pck4x.accounts_service.infrastructure.persistence.mapper;

import com.pck4x.accounts_service.domain.OutboxEvent;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.OutboxEventEntity;

import java.time.LocalDateTime;

public class OutboxEventMapper {

    public static final OutboxEventMapper INSTANCE = new OutboxEventMapper();

    private OutboxEventMapper() {}

    public OutboxEventEntity toEntity(OutboxEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setId(event.getId());
        entity.setTopic(event.getTopic());
        entity.setEventType(event.getEventType());
        entity.setPayload(event.getPayload());
        entity.setStatus(event.getStatus());
        entity.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt() : LocalDateTime.now());
        entity.setSentAt(event.getSentAt());
        return entity;
    }

    public OutboxEvent toDomain(OutboxEventEntity entity) {
        OutboxEvent event = new OutboxEvent();
        event.setId(entity.getId());
        event.setTopic(entity.getTopic());
        event.setEventType(entity.getEventType());
        event.setPayload(entity.getPayload());
        event.setStatus(entity.getStatus());
        event.setCreatedAt(entity.getCreatedAt());
        event.setSentAt(entity.getSentAt());
        return event;
    }
}
