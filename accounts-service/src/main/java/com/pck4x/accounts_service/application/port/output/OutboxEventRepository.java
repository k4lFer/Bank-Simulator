package com.pck4x.accounts_service.application.port.output;

import com.pck4x.accounts_service.domain.OutboxEvent;
import com.pck4x.accounts_service.domain.enums.OutboxStatus;

import java.util.List;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> findByStatus(OutboxStatus status);
}
