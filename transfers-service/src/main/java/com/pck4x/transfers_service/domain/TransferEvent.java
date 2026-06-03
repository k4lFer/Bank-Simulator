package com.pck4x.transfers_service.domain;

import com.pck4x.transfers_service.domain.enums.OutboxStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TransferEvent {
    private Long id;
    private UUID transferId;
    private String eventType;
    private String payload;
    private OutboxStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public TransferEvent() {}

    public TransferEvent(UUID transferId, String eventType, String payload) {
        this.transferId = transferId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}
