package com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaCardRepository extends JpaRepository<CardEntity, UUID> {
    List<CardEntity> findByUserId(UUID userId);
}
