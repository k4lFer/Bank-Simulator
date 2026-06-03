package com.pck4x.transfers_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.transfers_service.domain.enums.OutboxStatus;
import com.pck4x.transfers_service.infrastructure.persistence.jpa.entities.TransferEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaTransferEventRepository extends JpaRepository<TransferEventEntity, Long> {
    List<TransferEventEntity> findByStatus(OutboxStatus status);
}
