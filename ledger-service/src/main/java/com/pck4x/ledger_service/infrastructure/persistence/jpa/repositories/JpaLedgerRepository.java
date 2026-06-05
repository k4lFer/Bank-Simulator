package com.pck4x.ledger_service.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pck4x.ledger_service.infrastructure.persistence.jpa.entities.LedgerEntriesEntity;

public interface JpaLedgerRepository extends JpaRepository<LedgerEntriesEntity, Long> {
    List<LedgerEntriesEntity> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<LedgerEntriesEntity> findByTransferIdOrderByCreatedAtDesc(UUID transferId);
    List<LedgerEntriesEntity> findAllByOrderByCreatedAtDesc();
}
