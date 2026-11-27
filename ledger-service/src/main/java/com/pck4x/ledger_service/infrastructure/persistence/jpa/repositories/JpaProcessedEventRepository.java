package com.pck4x.ledger_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.ledger_service.infrastructure.persistence.jpa.entities.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {
    boolean existsByEventIdAndConsumer(String eventId, String consumer);
}
