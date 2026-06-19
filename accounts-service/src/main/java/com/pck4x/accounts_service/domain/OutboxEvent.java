package com.pck4x.accounts_service.domain;

import com.pck4x.accounts_service.domain.enums.OutboxStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OutboxEvent {
    private Long id;
    private String topic;
    private String eventType;
    private String payload;
    private OutboxStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public OutboxEvent() {}

    public OutboxEvent(String topic, String eventType, String payload) {
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}
