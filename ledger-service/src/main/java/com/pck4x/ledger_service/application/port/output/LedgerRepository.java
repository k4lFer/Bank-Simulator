package com.pck4x.ledger_service.application.port.output;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pck4x.ledger_service.domain.LedgerEntries;

public interface LedgerRepository {
    LedgerEntries save(LedgerEntries ledgerEntries);
    List<LedgerEntries> findByAccountNumber(String accountNumber);
    List<LedgerEntries> findByTransferId(UUID transferId);
    List<LedgerEntries> findAll();
    Optional<LedgerEntries> findById(Long id);
    Optional<LedgerEntries> findByTransferIdAndEntryTypeAndAccountNumber(UUID transferId, String entryType, String accountNumber);
    List<LedgerEntries> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<LedgerEntries> findByAccountNumberAndCreatedAtBetween(String accountNumber, LocalDateTime start, LocalDateTime end);
    List<LedgerEntries> findByCreatedAtBefore(LocalDateTime date);
}
