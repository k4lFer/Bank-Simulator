package com.pck4x.accounts_service.application.dto.response;

import com.pck4x.accounts_service.domain.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DepositResponse(
        String movementNumber,
        UUID accountId,
        String accountNumber,
        MovementType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {}
