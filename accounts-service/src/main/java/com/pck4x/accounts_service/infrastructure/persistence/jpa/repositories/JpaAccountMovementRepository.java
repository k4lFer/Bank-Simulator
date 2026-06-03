package com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.AccountMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaAccountMovementRepository extends JpaRepository<AccountMovementEntity, Long> {
    List<AccountMovementEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    int countByAccountId(UUID accountId);
}
