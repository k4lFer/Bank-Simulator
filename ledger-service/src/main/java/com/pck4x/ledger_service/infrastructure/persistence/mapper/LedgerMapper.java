package com.pck4x.ledger_service.infrastructure.persistence.mapper;

import com.pck4x.ledger_service.domain.LedgerEntries;
import com.pck4x.ledger_service.infrastructure.persistence.jpa.entities.LedgerEntriesEntity;

public class LedgerMapper {
    public static final LedgerMapper INSTANCE = new LedgerMapper();

    private LedgerMapper() {}

    public LedgerEntriesEntity toEntity(LedgerEntries ledger) {
        LedgerEntriesEntity entity = new LedgerEntriesEntity();
        entity.setId(ledger.getId());
        entity.setTransferId(ledger.getTransferId());
        entity.setAccountNumber(ledger.getAccountNumber());
        entity.setEntryType(ledger.getEntryType());
        entity.setAmount(ledger.getAmount());
        entity.setCurrency(ledger.getCurrency());
        entity.setCreatedAt(ledger.getCreatedAt());
        return entity;
    }

    public LedgerEntries toDomain(LedgerEntriesEntity entity) {
        LedgerEntries ledger = new LedgerEntries();
        ledger.setId(entity.getId());
        ledger.setTransferId(entity.getTransferId());
        ledger.setAccountNumber(entity.getAccountNumber());
        ledger.setEntryType(entity.getEntryType());
        ledger.setAmount(entity.getAmount());
        ledger.setCurrency(entity.getCurrency());
        ledger.setCreatedAt(entity.getCreatedAt());
        return ledger;
    }
}
