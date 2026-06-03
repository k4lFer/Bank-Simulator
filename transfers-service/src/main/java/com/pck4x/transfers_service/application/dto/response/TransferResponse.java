package com.pck4x.transfers_service.application.dto.response;

import com.pck4x.transfers_service.domain.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String currency,
        String description,
        TransferStatus status,
        LocalDateTime createdAt
) {
}
