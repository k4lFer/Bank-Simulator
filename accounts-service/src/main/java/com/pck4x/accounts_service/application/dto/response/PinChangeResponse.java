package com.pck4x.accounts_service.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PinChangeResponse(
        UUID id,
        String message,
        LocalDateTime updatedAt
) {}
