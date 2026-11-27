package com.pck4x.accounts_service.domain;

import com.pck4x.accounts_service.domain.enums.OutboxStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class OutboxEvent {
    private Long id;
    private String topic;
    private String eventType;
    private UUID eventId;
    private String payload;
    private OutboxStatus status;
    private int attempts;
    private LocalDateTime nextAttemptAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public OutboxEvent() {}

    public OutboxEvent(String topic, String eventType, String payload) {
        this(topic, eventType, payload, null);
    }

    public OutboxEvent(String topic, String eventType, String payload, UUID eventId) {
        this.topic = topic;
        this.eventType = eventType;
        this.eventId = eventId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
}
