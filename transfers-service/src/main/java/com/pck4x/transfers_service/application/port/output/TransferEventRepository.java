package com.pck4x.transfers_service.application.port.output;

import com.pck4x.transfers_service.domain.TransferEvent;
import com.pck4x.transfers_service.domain.enums.OutboxStatus;

import java.util.List;

public interface TransferEventRepository {
    TransferEvent save(TransferEvent event);
    List<TransferEvent> findByStatus(OutboxStatus status);
}
