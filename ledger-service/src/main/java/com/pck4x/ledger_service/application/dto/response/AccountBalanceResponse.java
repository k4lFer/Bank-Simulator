package com.pck4x.ledger_service.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountBalanceResponse(
        String accountNumber,
        String currency,
        BigDecimal balance,
        LocalDateTime calculatedAt
) {}
