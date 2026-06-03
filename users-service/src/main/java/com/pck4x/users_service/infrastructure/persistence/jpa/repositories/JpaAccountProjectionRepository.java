package com.pck4x.users_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.users_service.infrastructure.persistence.jpa.entities.AccountProjectionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaAccountProjectionRepository extends JpaRepository<AccountProjectionEntity, UUID> {
    List<AccountProjectionEntity> findByUserId(UUID userId);
    Page<AccountProjectionEntity> findByUserId(UUID userId, Pageable pageable);
}
