package com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.CardAccountEntity;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.CardAccountId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaCardAccountRepository extends JpaRepository<CardAccountEntity, CardAccountId> {
    List<CardAccountEntity> findByCardId(UUID cardId);
    List<CardAccountEntity> findByAccountId(UUID accountId);
    void deleteByCardIdAndAccountId(UUID cardId, UUID accountId);
}
