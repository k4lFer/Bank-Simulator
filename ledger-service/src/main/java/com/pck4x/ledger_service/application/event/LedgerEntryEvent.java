package com.pck4x.ledger_service.application.event;

import com.pck4x.ledger_service.domain.LedgerEntries;

public record LedgerEntryEvent(LedgerEntries entry) {}
