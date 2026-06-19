package com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.accounts_service.domain.enums.OutboxStatus;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {
    List<OutboxEventEntity> findByStatusOrderByIdAsc(OutboxStatus status);
}
