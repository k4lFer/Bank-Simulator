package com.pck4x.accounts_service.application.dto.response;

import com.pck4x.sharedcontracts.enums.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountDetailResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        UUID userId,
        String maskedPin6,
        String maskedPin4,
        int movementCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
