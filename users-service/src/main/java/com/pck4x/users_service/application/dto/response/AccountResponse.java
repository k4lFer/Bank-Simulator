package com.pck4x.users_service.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String currency,
        String status,
        UUID userId,
        LocalDateTime createdAt
) {}
