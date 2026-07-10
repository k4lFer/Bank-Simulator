package com.pck4x.accounts_service.application.dto.response;

import com.pck4x.accounts_service.domain.enums.CardStatus;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID userId,
        String pan,
        YearMonth expiryDate,
        CardStatus status,
        LocalDateTime createdAt
) {}
