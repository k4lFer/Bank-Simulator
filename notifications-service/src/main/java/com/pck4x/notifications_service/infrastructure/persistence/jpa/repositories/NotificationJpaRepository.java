package com.pck4x.notifications_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.notifications_service.infrastructure.persistence.jpa.entities.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Pageable pageable, UUID userId);
    long countByUserIdAndReadFalse(UUID userId);
}
