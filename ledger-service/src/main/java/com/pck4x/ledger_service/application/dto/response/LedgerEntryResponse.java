package com.pck4x.ledger_service.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LedgerEntryResponse(
        Long id,
        UUID transferId,
        String accountNumber,
        String entryType,
        BigDecimal amount,
        String currency,
        LocalDateTime createdAt
) {}
