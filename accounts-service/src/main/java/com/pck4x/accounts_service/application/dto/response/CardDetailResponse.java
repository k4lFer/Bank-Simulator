package com.pck4x.accounts_service.application.dto.response;

import com.pck4x.accounts_service.domain.enums.CardStatus;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record CardDetailResponse(
        UUID id,
        UUID userId,
        String pan,
        YearMonth expiryDate,
        CardStatus status,
        List<LinkedAccountInfo> accounts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record LinkedAccountInfo(UUID accountId, String accountNumber, String currency, boolean primary) {}
}
