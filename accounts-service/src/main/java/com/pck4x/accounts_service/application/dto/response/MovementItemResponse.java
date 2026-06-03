package com.pck4x.accounts_service.application.dto.response;

import com.pck4x.accounts_service.domain.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovementItemResponse(
        Long id,
        String movementNumber,
        MovementType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {}
