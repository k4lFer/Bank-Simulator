package com.pck4x.accounts_service.application.dto.response;

import com.pck4x.sharedcontracts.enums.AccountStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountStatusResponse(
        UUID id,
        String accountNumber,
        AccountStatus status,
        LocalDateTime updatedAt
) {}
