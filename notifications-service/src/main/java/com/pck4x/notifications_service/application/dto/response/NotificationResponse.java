package com.pck4x.notifications_service.application.dto.response;

import com.pck4x.notifications_service.domain.enums.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String message,
        BigDecimal amount,
        String currency,
        String relatedAccount,
        boolean read,
        LocalDateTime createdAt
) {}
