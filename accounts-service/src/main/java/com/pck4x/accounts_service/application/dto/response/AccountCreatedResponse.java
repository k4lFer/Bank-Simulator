package com.pck4x.accounts_service.application.dto.response;

import com.pck4x.sharedcontracts.enums.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountCreatedResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        LocalDateTime createdAt
) {}
